-- schema.lua
local typedefs = require "kong.db.schema.typedefs"

local PLUGIN_NAME = "data-masking"

return {
  name = PLUGIN_NAME,
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    { config = {
        type = "record",
        fields = {
          {
            field_names = {
              type = "string",
              required = true,
              default = "nikSap,name",
              description = "Comma-separated field names to mask (supports nested fields with dot notation, e.g.: data.nik)"
            }
          },
          {
            mask_chars = {
              type = "string",
              required = true,
              default = "x,x",
              description = "Comma-separated mask characters for each field"
            }
          },
          {
            expose_chars = {
              type = "string",
              required = true,
              default = "2,1",
              description = "Comma-separated numbers of characters to expose"
            }
          },
          {
            patterns = {
              type = "string",
              required = false,
              default = "^%s*%d+%s*$;^[A-Za-z%s,%.]+$",
              description = "Semicolon-separated patterns for validation"
            }
          }
        }
    }}
  }
}
