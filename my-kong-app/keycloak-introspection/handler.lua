--[[
    KeycloakIntrospectionHandler - Kong plugin for Keycloak token introspection

    This plugin validates OAuth2 access tokens by introspecting them against a Keycloak server.
    It checks if the token is active and adds user information to request headers.

    @module KeycloakIntrospectionHandler
    @version 1.0.0
    @priority 1200

    Configuration parameters:
    - keycloak_introspection_url: URL of the Keycloak introspection endpoint
    - client_id: OAuth2 client ID for introspection
    - client_secret: OAuth2 client secret for introspection

    Functionality:
    1. Extracts access token from Authorization header
    2. Makes introspection request to Keycloak server
    3. Validates token status and activity
    4. Adds user information to request headers:
       - X-User-Id: User's subject identifier
       - X-Username: User's username

    Error responses:
    - 401 Unauthorized: Missing or invalid token
    - 500 Internal Server Error: Introspection request failed

    @usage
    Configure the plugin in Kong with Keycloak credentials and introspection URL
--]]
-- keycloak-introspection/handler.lua

local http = require("resty.http")
local cjson = require("cjson")

local KeycloakIntrospectionHandler = {
  VERSION  = "1.0.0",
  PRIORITY = 1200,
}

function KeycloakIntrospectionHandler:access(config)

    -- Get the access token from the request headers
    local access_token= ngx.var.http_authorization

    if not access_token then
        ngx.log(ngx.ERR, "Access token not found in request headers")
        return ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end
    -- Introspect the access token with Keycloak

    local introspection_url = config.keycloak_introspection_url
    --local access_token = ngx.var.http_authorization
    local httpc = http.new()


     local headers = {
     ["Content-Type"] = "application/x-www-form-urlencoded",
        ["Authorization"] = "Basic " .. ngx.encode_base64(config.client_id .. ":" .. config.client_secret),
     } 

    local body = "token=" .. access_token

    local request_options = {
     method = "POST",
     body = body,
     headers = headers,
    }

    local res, err = httpc:request_uri(introspection_url, request_options)
    
    ngx.log(ngx.NOTICE, "Entering access function")
    ngx.log(ngx.NOTICE, "body ", cjson.encode(request_options))
    ngx.log(ngx.NOTICE, "Plugin Configuration :", cjson.encode(config))

    if not res then
        ngx.log(ngx.ERR, "Failed to introspect token: ", err)
        return ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
    end

    if res.status ~= 200 then
        ngx.log(ngx.ERR, "Token introspection failed with status: ", res.status)
        return ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end

    -- Parse the introspection response
    local introspection_result = cjson.decode(res.body)
    ngx.log(ngx.NOTICE, "Introspection result: ", res.body)

    -- Check if the token is active
    if not introspection_result.active then
        ngx.log(ngx.ERR, "Access token is not active")
        return ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end

    -- Add introspection result to request headers
    ngx.req.set_header("X-User-Id", introspection_result.sub)
    ngx.req.set_header("X-Username", introspection_result.username)

    ngx.log(ngx.INFO, "Token introspection successful")

    -- Close the HTTP connection
    httpc:close()
end

return KeycloakIntrospectionHandler