--- Keycloak Introspection Plugin Schema
-- Defines the configuration schema for the Keycloak Introspection plugin.
-- This plugin validates OAuth 2.0 tokens by introspecting them against a Keycloak server.
--
-- @module keycloak-introspection.schema
--
-- Fields:
-- @field consumer No consumer needed for this plugin
-- @field protocols Only supports HTTP protocol
-- @field config Configuration settings for the plugin:
--   - keycloak_introspection_url: The URL of the Keycloak token introspection endpoint (required, must be http/https)
--   - client_id: Client ID for authentication with Keycloak (required, defaults to "check")
--   - client_secret: Client secret for authentication with Keycloak (required)
-- @author edoaurahman@gmail.com
local typedefs = require "kong.db.schema.typedefs"

return {
    name = "keycloak-introspection",
    fields = {{
        consumer = typedefs.no_consumer
    }, {
        protocols = typedefs.protocols_http
    }, {
        config = {
            type = "record",
            fields = {{
                keycloak_introspection_url = {
                    type = "string",
                    required = true,
                    match = "https?://.+"
                }
            }, {
                client_id = {
                    type = "string",
                    required = true,
                    default = "check"
                }
            }, {
                client_secret = {
                    type = "string",
                    required = true
                }
            }}
        }
    }}
}
