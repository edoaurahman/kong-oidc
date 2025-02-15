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
                    description = "Access token for the API"
                }
            }, {
                refresh_token = {
                    type = "string",
                    required = true,
                    description = "Refresh token for the API"
                }
            }, {
                refresh_endpoint = {
                    type = "string",
                    required = true,
                    description = "Endpoint untuk for refresh token"
                }
            }, {
                refresh_method = {
                    type = "string",
                    default = "POST",
                    description = "HTTP method for refresh token",
                    one_of = {"GET", "POST", "PUT", "DELETE"}
                }
            }, {
                refresh_body = {
                    type = "string",
                    default = "$refresh_token will be replaced with the actual refresh token",
                    required = false,
                    description = "Body request for refresh token"
                }
            }, {
                content_type = {
                    type = "string",
                    default = "application/json",
                    description = "Content-Type request for refresh token",
                    one_of = {"application/json", "application/x-www-form-urlencoded"}
                }
            }, {
                header_authorization = {
                    type = "string",
                    required = true,
                    default = 'Authorization: Bearer $access_token | $access_token will be replaced with the actual access token',
                    description = "Custom header for authorization"
                }
            }}
        }
    }}
}
