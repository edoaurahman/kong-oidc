local http = require "resty.http"
local cjson = require "cjson"
-- local ngx_shared = ngx.shared
local redis = require "resty.redis"

local TokenManagerPlugin = {
    PRIORITY = 1000,
    VERSION = "0.1"
}

-- Add this helper function at the top with other helper functions
local function parse_header_template(template, token)
    -- Replace $access_token with actual token
    local header_value = template:gsub("%$access_token", token)

    -- Split into header name and value
    local header_name, header_content = header_value:match("^([^:]+):%s*(.+)$")

    if not header_name or not header_content then
        kong.log.err("Invalid header template format: ", template)
        return nil, nil
    end

    return header_name:gsub("^%s*(.-)%s*$", "%1"), -- trim whitespace
    header_content:gsub("^%s*(.-)%s*$", "%1") -- trim whitespace
end

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

    -- If value is nil, delete the key in Redis
    if value == nil then
        local ok, err = red:del(full_key)
        if not ok then
            kong.log.err("Failed to delete key from Redis: ", err)
        end
        return
    end

    -- Otherwise, store the string version of value
    local ok, err = red:set(full_key, tostring(value))
    if not ok then
        kong.log.err("Failed to store token in Redis: ", err)
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
local function refresh_token(conf, use_stored_token)
    local httpc = http.new()
    local headers = {
        ["Content-Type"] = conf.content_type
    }

    local body
    local refresh_token = conf.refresh_token

    -- Only use stored token if explicitly requested
    if use_stored_token then
        local stored_refresh_token = get_token_from_redis("refresh_token")
        if stored_refresh_token then
            refresh_token = stored_refresh_token
        end
    end

    -- If no refresh token available
    if not refresh_token then
        return nil, nil, "No refresh token available"
    end

    -- Prepare request body
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

    -- Make the request
    local res, err = httpc:request_uri(conf.refresh_endpoint, {
        method = conf.refresh_method,
        body = body,
        headers = headers,
        ssl_verify = false
    })

    if not res then
        kong.log.err("Failed to refresh token: ", err)
        return nil, nil, err
    end

    -- Parse response
    local new_token, new_refresh_token
    if res.status == 200 then
        if conf.content_type == "application/json" then
            local parsed_body, err = cjson.decode(res.body)
            if parsed_body then
                new_token = parsed_body.access_token
                new_refresh_token = parsed_body.refresh_token
            end
        elseif conf.content_type == "application/x-www-form-urlencoded" then
            local args = ngx.decode_args(res.body)
            new_token = args.access_token
            new_refresh_token = args.refresh_token
        end
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

    if not access_token or access_token == "" then
        kong.log.err("Access token not available!")
        return kong.response.exit(401, {
            message = "Access token not available"
        })
    end
    -- Parse and set the custom header
    local header_name, header_value = parse_header_template(conf.header_authorization, access_token)
    if not header_name then
        kong.log.err("Failed to parse header template")
        return kong.response.exit(500, {
            message = "Invalid header configuration"
        })
    end
    kong.log("Setting custom authorization header: ", header_name, " with value: ", header_value)
    kong.service.request.set_header(header_name, header_value)
end

-- Fungsi utama pada response
function TokenManagerPlugin:response(conf)
    kong.log("Response phase started")
    local status = kong.response.get_status()
    -- Add a custom header to track retry attempts
    local retry_count = kong.request.get_header("X-Token-Refresh-Retry")
    if retry_count and tonumber(retry_count) >= 1 then
        kong.log.warn("Maximum token refresh attempts reached")
        return kong.response.exit(500, {
            message = "Token refresh failed, please re-authenticate"
        })
    end
    if status == 401 then
        kong.log("Token expired, attempting to refresh...")

        -- First try with configuration token
        local new_token, new_refresh_token, err = refresh_token(conf, true)
        -- If first attempt fails, try with stored token
        if not new_token then
            kong.log("First refresh attempt failed, trying with configure token...")
            new_token, new_refresh_token, err = refresh_token(conf, false)
        end

        if not new_token then
            kong.log.err("All refresh attempts failed: ", err)
            return kong.response.exit(401, {
                message = "Token refresh failed, please re-authenticate"
            })
        end

        kong.log("new_token: ", new_token)
        kong.log("new_refresh_token: ", new_refresh_token)
        kong.log("err: ", err)
        -- Store successful tokens
        store_token_in_redis("access_token", new_token)
        if new_refresh_token then
            store_token_in_redis("refresh_token", new_refresh_token)
        end

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
        kong.log("Upstream url : ", upstream_url)
        local header_name, header_value = parse_header_template(conf.header_authorization, new_token)
        local res_retry, err = httpc:request_uri(upstream_url, {
            method = kong.request.get_method(),
            headers = {
                [header_name] = header_value,
                ["Host"] = upstream_host,
                ["X-Token-Refresh-Retry"] = "1"
            },
            body = kong.request.get_raw_body(),
            ssl_verify = false
        })

        kong.log("res_retry: ", res_retry)
        kong.log("err while retrying request: ", err)

        if not res_retry then
            kong.log("Failed to retry request: ", err)
            return kong.response.exit(500, {
                message = "Failed to retry request"
            })
        end

        return kong.response.exit(res_retry.status, res_retry.body, res_retry.headers)
    end
end

return TokenManagerPlugin
