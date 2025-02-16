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
                    required = true,
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
