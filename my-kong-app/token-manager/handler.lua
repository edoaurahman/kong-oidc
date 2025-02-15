local http = require "resty.http"
local cjson = require "cjson"
local ngx_shared = ngx.shared

local TokenManagerPlugin = {
    PRIORITY = 1000,
    VERSION = "0.1"
}

-- Fungsi untuk melakukan substitusi anotasi
local function substitute_token_in_body(body, refresh_token)
    if type(body) == "table" then
        for k, v in pairs(body) do
            if type(v) == "string" and v == "$refresh_token" then
                body[k] = refresh_token
            elseif type(v) == "table" then
                substitute_token_in_body(v, refresh_token)
            end
        end
    end
end

-- Fungsi untuk melakukan refresh token
local function refresh_token(conf)
    local httpc = http.new()
    local headers = {
        ["Content-Type"] = conf.content_type
    }

    local body
    local refresh_token = conf.refresh_token

    local shared_dict = ngx_shared.kong_token_store
    if shared_dict then
        local stored_refresh_token = shared_dict:get("refresh_token")
        if stored_refresh_token then
            refresh_token = stored_refresh_token
        end
    end

    -- Jika JSON, lakukan substitusi
    if conf.content_type == "application/json" then
        local parsed_body = cjson.decode(conf.refresh_body)
        substitute_token_in_body(parsed_body, refresh_token)
        body = cjson.encode(parsed_body)
    elseif conf.content_type == "application/x-www-form-urlencoded" then
        body = ngx.encode_args(conf.refresh_body)
    end

    local res, err = httpc:request_uri(conf.refresh_endpoint, {
        method = conf.refresh_method,
        body = body,
        headers = headers
    })

    if not res then
        kong.log("Failed to refresh token: ", err)
        return nil, nil, err
    end

    local new_token
    local refresh_token
    if conf.content_type == "application/json" then
        local parsed_body = cjson.decode(res.body)
        new_token = parsed_body.access_token
        refresh_token = parsed_body.refresh_token
    elseif conf.content_type == "application/x-www-form-urlencoded" then
        local args = ngx.decode_args(res.body)
        new_token = args.access_token
        refresh_token = args.refresh_token
    end

    return new_token, refresh_token, nil
end

-- Fungsi utama pada request
function TokenManagerPlugin:access(conf)
    kong.log("Access phase started")
    local shared_dict = ngx_shared.kong_token_store
    local access_token = conf.access_token

    if shared_dict then
        local stored_token = shared_dict:get("access_token")
        if stored_token then
            access_token = stored_token
        end
    end

    kong.service.request.set_header("Authorization", "Bearer " .. access_token)
    kong.log("Authorization header set with access token:", access_token)
end

-- Fungsi utama pada response
function TokenManagerPlugin:response(conf)
    kong.log("Response phase started")
    local res = kong.response.get_status()
    if res == 401 then
        kong.log("Token expired, attempting to refresh...")

        local new_token, new_refresh_token, err = refresh_token(conf)
        kong.log("new_token: ", new_token)
        kong.log("new_refresh_token: ", new_refresh_token)
        if not new_token then
            kong.log("Failed to refresh token: ", err)
            return kong.response.exit(500, {
                message = "Failed to refresh token"
            })
        end

        -- Store the new tokens in shared dictionary
        local shared_dict = ngx_shared.kong_token_store
        if shared_dict then
            local ok, err = shared_dict:set("access_token", new_token)
            if not ok then
                kong.log("Failed to store new token: ", err)
            end

            if new_refresh_token then
                ok, err = shared_dict:set("refresh_token", new_refresh_token)
                if not ok then
                    kong.log("Failed to store new refresh token: ", err)
                end
            end
        end

        kong.log("Stored new access token: ", shared_dict:get("access_token"))
        kong.log("Stored new refresh token: ", shared_dict:get("refresh_token"))

        -- Retry the request with the new token
        local httpc = http.new()
        local upstream_scheme = kong.request.get_scheme()
        local upstream_host = kong.request.get_host()
        local upstream_port = kong.request.get_port()
        local upstream_path = kong.request.get_path()
        local upstream_query = kong.request.get_raw_query()

        kong.log("Retrying request with new token: ", new_token)

        -- Construct full URL
        local upstream_url = string.format("%s://%s:%d%s", upstream_scheme, upstream_host, upstream_port, upstream_path)
        if upstream_query and upstream_query ~= "" then
            upstream_url = upstream_url .. "?" .. upstream_query
        end

        local res, err = httpc:request_uri(upstream_url, {
            method = kong.request.get_method(),
            headers = {
                ["Authorization"] = "Bearer " .. new_token,
                ["Host"] = upstream_host
            },
            body = kong.request.get_raw_body()
        })

        if not res then
            kong.log("Failed to retry request: ", err)
            return kong.response.exit(500, {
                message = "Failed to retry request"
            })
        end

        return kong.response.exit(res.status, res.body, res.headers)
    end
end

return TokenManagerPlugin
