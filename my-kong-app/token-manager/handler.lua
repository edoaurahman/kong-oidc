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

-- Fungsi untuk menutup koneksi Redis
local function close_redis(red)
    if not red then
        return
    end
    local ok, err = red:set_keepalive(60000, 100)
    if not ok then
        kong.log.err("Failed to set Redis keepalive: ", err)
    end
end

-- Fungsi untuk Menyimpan Token dengan Prefix
local function store_token_in_redis(key, value)
    local red = connect_to_redis()
    if not red then
        return
    end

    local service_prefix = get_service_prefix()
    local full_key = service_prefix .. key
    kong.log("Storing token in Redis with key: ", full_key)
    kong.log("Value: ", value)
    local ok, err = red:set(full_key, value)
    if not ok then
        kong.log.err("Failed to store token in Redis: ", err)
        return
    end
    close_redis(red)
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
    close_redis(red)
    return tostring(res)
end

-- Fungsi untuk melakukan substitusi anotasi
local function substitute_token(header_value, new_token, placeholder)
    kong.log("Original header_value type: ", type(header_value))
    kong.log("Placeholder to replace: ", placeholder)
    kong.log("New token: ", new_token)

    -- Handle table (JSON) values
    if type(header_value) == "table" then
        local json_str = cjson.encode(header_value)
        kong.log("Original JSON string: ", json_str)

        -- Escape special characters in placeholder
        local escaped_placeholder = placeholder:gsub("([%^%$%(%)%%%.%[%]%*%+%-%?])", "%%%1")

        -- Replace placeholder in JSON string
        local new_json_str = json_str:gsub(escaped_placeholder, new_token)
        kong.log("Modified JSON string: ", new_json_str)

        -- Decode back to table
        local success, result = pcall(cjson.decode, new_json_str)
        if not success then
            kong.log.err("Failed to decode modified JSON: ", result)
            return header_value
        end

        return result
    end

    -- Handle string values (original behavior)
    if type(header_value) == "string" then
        local escaped_placeholder = placeholder:gsub("([%^%$%(%)%%%.%[%]%*%+%-%?])", "%%%1")
        local new_header_value, n = header_value:gsub(escaped_placeholder, new_token)

        kong.log("New header_value after substitution: ", new_header_value)
        kong.log("Number of replacements made: ", n)

        return new_header_value
    end

    -- Return original value for unsupported types
    kong.log.warn("Unsupported header_value type: ", type(header_value))
    return header_value
end

-- Fungsi untuk melakukan refresh token
local function refresh_token(conf, use_stored_token)
    -- delete all Tokens
    store_token_in_redis("access_token", nil)
    store_token_in_redis("refresh_token", nil)
    local httpc = http.new()
    local headers = {
        ["Content-Type"] = conf.content_type
    }

    local body
    local stored_refresh_token = get_token_from_redis("refresh_token") or conf.refresh_token
    local refresh_token = conf.refresh_token

    if use_stored_token then
        refresh_token = stored_refresh_token
    end

    -- Jika JSON, lakukan substitusi
    if conf.content_type == "application/json" then
        local parsed_body, err = cjson.decode(conf.refresh_body)
        if not parsed_body then
            kong.log.err("Failed to decode JSON body: ", err)
            return nil, nil, "Failed to decode JSON body"
        end
        parsed_body = substitute_token(parsed_body, refresh_token, "$refresh_token")
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
        headers = headers,
        ssl_verify = conf.ssl_verify
    })
    if res.status ~= 200 then
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
    kong.log("Save token from response")
    store_token_in_redis("access_token", new_token)
    kong.log("Access token saved, ", new_token)
    store_token_in_redis("refresh_token", new_refresh_token)
    kong.log("Refresh token saved, ", new_refresh_token)
    return new_token, new_refresh_token, nil
end

local function is_refresh_endpoint(conf)
    local refresh_path = kong.router.get_route().paths[1] -- Ambil path dari route
    local request_path = kong.request.get_path()
    return request_path:match(refresh_path .. "/?$")
end

-- Fungsi utama pada request
function TokenManagerPlugin:access(conf)
    -- Skip token management for refresh endpoint
    if is_refresh_endpoint(conf) then
        kong.log.debug("Skipping token management for refresh endpoint")
        return
    end
    kong.log("Access phase started")
    local stored_access_token = get_token_from_redis("access_token")
    local access_token = conf.access_token

    if stored_access_token and stored_access_token ~= ngx.null then
        access_token = stored_access_token
    end

    if not access_token or access_token == "" then
        kong.log.err("Access not available!")
        return kong.response.exit(401, {
            message = "Access not available"
        })
    end

    kong.log("Authorization header set with access token:", access_token)
    local token_value = conf.header_value
    token_value = substitute_token(token_value, access_token, "$access_token")
    kong.service.request.set_header(conf.header_key, token_value)
end

-- Fungsi utama pada response
function TokenManagerPlugin:response(conf)
    if is_refresh_endpoint(conf) then
        kong.log.debug("Skipping token refresh for refresh endpoint")
        return
    end

    kong.log("Response phase started")
    local status = kong.response.get_status()

    -- Batas maksimal retry
    local max_retries = 2  -- Sesuaikan dengan kebutuhan
    local retry_count = 0  -- Variabel untuk melacak jumlah retry

    -- Fungsi untuk melakukan retry request
    local function retry_request()
        retry_count = retry_count + 1
        if retry_count > max_retries then
            kong.log.err("Max retry attempts reached")
            return kong.response.exit(500, {
                message = "Max retry attempts reached, please contact support"
            })
        end

        kong.log("Token expired, attempting to refresh... (Retry attempt: " .. retry_count .. ")")

        -- 1. Refresh Token
        local new_token, new_refresh_token, err = refresh_token(conf, true)
        if not new_token or not new_refresh_token then
            store_token_in_redis("access_token", nil)
            store_token_in_redis("refresh_token", nil)
            new_token, new_refresh_token, err = refresh_token(conf, false)
        end

        if not new_token then
            kong.log.err("Failed to refresh token: ", err)
            return kong.response.exit(500, {
                message = "Failed to refresh token, please re-authenticate or contact support"
            })
        end

        -- 2. Update Headers dengan Token Baru
        local request_headers = kong.request.get_headers()
        request_headers[conf.header_key] = substitute_token(conf.header_value, new_token, "$access_token")
        request_headers["Authorization"] = nil  -- Hapus header lama (jika ada)

        -- 3. Lakukan Request Ulang ke Upstream
        local httpc = http.new()
        local upstream_url = kong.request.get_scheme() .. "://" .. kong.request.get_host() .. kong.request.get_path()
        local request_method = kong.request.get_method()
        local request_body = kong.request.get_raw_body()

        kong.log("Retrying request to upstream URL: ", upstream_url)
        local res, err = httpc:request_uri(upstream_url, {
            method = request_method,
            body = request_body,
            headers = request_headers,
            ssl_verify = conf.ssl_verify
        })

        if not res then
            kong.log.err("Failed to retry request: ", err)
            return kong.response.exit(500, {
                message = "Failed to retry request, please contact support"
            })
        end

        -- 4. Periksa kembali status respons
        if res.status == 401 then
            -- Jika masih 401, lakukan retry lagi
            return retry_request()
        else
            -- Jika berhasil, kirim respons ke klien
            return kong.response.exit(res.status, res.body, res.headers)
        end
    end

    -- Mulai proses retry jika status 401
    if status == 401 then
        return retry_request()
    end
end

return TokenManagerPlugin
