--[[
Schema configuration for the Token Manager Kong plugin.

This plugin manages access tokens and token refresh operations for API authentication.

Fields:
- consumer: No consumer configuration required
- protocols: HTTP protocol configuration

Config fields:
- access_token (string, required): Access token used for authentication
- refresh_token (string, optional): Token used to refresh the access token
- refresh_endpoint (string, required): Endpoint URL for refreshing access token
- refresh_method (string, default="POST"): HTTP method for refresh token request
                                         Allowed: GET, POST, PUT, DELETE
- refresh_body (string, optional): Request body for refresh token
- content_type (string, default="application/json"): Content-Type header for refresh request
                                                    Allowed: application/json, 
                                                            application/x-www-form-urlencoded
- header_key (string, required, default="Authorization"): Header key for access token
- header_value (string, required, default="Bearer $access_token"): Header value format
- ssl_verify (boolean, required, default=false): Enable/disable SSL verification

@module token-manager
@author edoaurahman@gmail.com
]]--
local typedefs = require "kong.db.schema.typedefs"

return {
    name = "token-manager",
    fields = {{
        consumer = typedefs.no_consumer
    }, {
        protocols = typedefs.protocols_http
    }, {
        config = {
            type = "record",
            fields = {{
                access_token = {
                    type = "string",
                    required = true,
                    description = "Access token untuk autentikasi"
                }
            }, {
                refresh_token = {
                    type = "string",
                    required = false,
                    description = "Refresh token untuk memperbarui access token"
                }
            }, {
                refresh_endpoint = {
                    type = "string",
                    required = true,
                    description = "Endpoint untuk memperbarui access token"
                }
            }, {
                refresh_method = {
                    type = "string",
                    default = "POST",
                    description = "HTTP method untuk refresh token",
                    one_of = {"GET", "POST", "PUT", "DELETE"}
                }
            }, {
                refresh_body = {
                    type = "string",
                    required = false,
                    description = "Body untuk request refresh token"
                }
            }, {
                content_type = {
                    type = "string",
                    default = "application/json",
                    description = "Content-Type untuk request refresh token",
                    one_of = {"application/json", "application/x-www-form-urlencoded"}
                }
            }, {
                header_key = {
                    type = "string",
                    required = true,
                    default = "Authorization",
                    description = "Set header key untuk access token"
                }
            }, {
                header_value = {
                    type = "string",
                    required = true,
                    default = "Bearer $access_token",
                    description = "Set value untuk header key"
                }
            }, {
                ssl_verify = {
                    type = "boolean",
                    required = true,
                    default = false,
                    description = "Set SSL verify"
                }
            }}
        }
    }}
}
