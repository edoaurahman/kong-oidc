local http = require "resty.http"
local cjson = require "cjson"
-- local ngx_shared = ngx.shared
local redis = require "resty.redis"

local TokenManagerPlugin = {
    PRIORITY = 1000,
    VERSION = "0.1"
}

-- Fungsi untuk mendapatkan prefix service
local function get_service_prefix()
    local service = kong.router.get_service()
    if service and service.name then
        return service.name .. ":"
    end
    return "default:"
end

-- Fungsi untuk Connect ke Redis
local function connect_to_redis()
    local red = redis:new()
    red:set_timeout(1000) -- 1 detik timeout

    -- Sesuaikan dengan konfigurasi Redis Anda
    local ok, err = red:connect("kong-redis", 6379)
    if not ok then
        kong.log("Failed to connect to Redis: ", err)
        return nil
    end
    return red
end

-- Fungsi untuk Menyimpan Token dengan Prefix
local function store_token_in_redis(key, value)
    local red = connect_to_redis()
    if not red then
        return
    end

    local service_prefix = get_service_prefix()
    local full_key = service_prefix .. key

    local ok, err = red:set(full_key, value)
    if not ok then
        kong.log.err("Failed to store token in Redis: ", err)
        return
    end
end

-- Fungsi untuk Mengambil Token dari Redis
local function get_token_from_redis(key)
    local red = connect_to_redis()
    if not red then
        return nil
    end

    local service_prefix = get_service_prefix()
    local full_key = service_prefix .. key

    local res, err = red:get(full_key)
    if not res then
        kong.log.err("Failed to get token from Redis: ", err)
        return nil
    end

    -- Handle NULL values from Redis
    if res == ngx.null then
        kong.log.debug("Token not found in Redis")
        return nil
    end

    -- Convert response to string to ensure proper type
    return tostring(res)
end

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
    local stored_refresh_token = get_token_from_redis("refresh_token")
    local refresh_token = conf.refresh_token

    if stored_refresh_token then
        refresh_token = stored_refresh_token
    end
    -- local shared_dict = ngx_shared.kong_token_store
    -- if shared_dict then
    --     local stored_refresh_token = shared_dict:get("refresh_token")
    --     if stored_refresh_token then
    --         refresh_token = stored_refresh_token
    --     end
    -- end

    -- Jika JSON, lakukan substitusi
    if conf.content_type == "application/json" then
        local parsed_body, err = cjson.decode(conf.refresh_body)
        if not parsed_body then
            kong.log.err("Failed to decode JSON body: ", err)
            return nil, nil, "Failed to decode JSON body"
        end
        substitute_token_in_body(parsed_body, refresh_token)
        body, err = cjson.encode(parsed_body)
        if not body then
            kong.log.err("Failed to encode JSON body: ", err)
            return nil, nil, "Failed to encode JSON body"
        end
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
        -- delete the stored tokens
        store_token_in_redis("access_token", nil)
        store_token_in_redis("refresh_token", nil)
        return nil, nil, err
    end

    local new_token
    local new_refresh_token
    if conf.content_type == "application/json" then
        local parsed_body, err = cjson.decode(res.body)
        if not parsed_body then
            kong.log.err("Failed to decode JSON response body: ", err)
            return nil, nil, "Failed to decode JSON response body"
        end
        new_token = parsed_body.access_token
        new_refresh_token = parsed_body.refresh_token
    elseif conf.content_type == "application/x-www-form-urlencoded" then
        local args = ngx.decode_args(res.body)
        new_token = args.access_token
        new_refresh_token = args.refresh_token
    end

    -- handle if new token is not present
    if not new_token then
        store_token_in_redis("access_token", nil)
        store_token_in_redis("refresh_token", nil)
        kong.log.err("Failed to get new token from response")
        return conf.access_token, conf.refresh_token, "Failed to get new token from response"
    end

    return new_token, new_refresh_token, nil
end

-- Fungsi utama pada request
function TokenManagerPlugin:access(conf)
    kong.log("Access phase started")
    local stored_access_token = get_token_from_redis("access_token")
    local access_token = conf.access_token

    if stored_access_token then
        access_token = stored_access_token
    end

    -- if shared_dict then
    --     local stored_token = shared_dict:get("access_token")
    --     if stored_token then
    --         access_token = stored_token
    --     end
    -- end
    if not access_token or access_token == "" then
        kong.log.err("Access token tidak tersedia!")
        return kong.response.exit(401, {
            message = "Access token tidak tersedia"
        })
    end
    kong.log("Authorization header set with access token:", access_token)
    kong.service.request.set_header("Authorization", "Bearer " .. access_token)
end

-- Fungsi utama pada response
function TokenManagerPlugin:response(conf)
    kong.log("Response phase started")
    local status = kong.response.get_status()
    if status == 401 then
        kong.log("Token expired, attempting to refresh...")

        local new_token, new_refresh_token, err = refresh_token(conf)
        kong.log("new_token: ", new_token)
        kong.log("new_refresh_token: ", new_refresh_token)
        kong.log("err: ", err)
        if not new_token then
            kong.log("Failed to refresh token: ", err)
            return kong.response.exit(500, {
                message = "Failed to refresh token, please re-authenticate or contact support"
            })
        end

        -- Store the new tokens in redis
        store_token_in_redis("access_token", new_token)
        store_token_in_redis("refresh_token", new_refresh_token)

        -- Store the new tokens in shared dictionary
        -- local shared_dict = ngx_shared.kong_token_store
        -- if shared_dict then
        --     local ok, err = shared_dict:set("access_token", new_token)
        --     if not ok then
        --         kong.log("Failed to store new token: ", err)
        --     end

        --     if new_refresh_token then
        --         ok, err = shared_dict:set("refresh_token", new_refresh_token)
        --         if not ok then
        --             kong.log("Failed to store new refresh token: ", err)
        --         end
        --     end
        -- end

        -- kong.log("Stored new access token: ", shared_dict:get("access_token"))
        -- kong.log("Stored new refresh token: ", shared_dict:get("refresh_token"))

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
