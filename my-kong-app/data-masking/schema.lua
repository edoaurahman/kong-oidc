local typedefs = require "kong.db.schema.typedefs"

local plugin_name = "data-masking"

--TODO: Add validation
local string_array = {
  type = "array",
  default = {},
  required = true,
  elements = { type = "string" },
}

return {
  name = plugin_name,
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    { config = {
        type = "record",
        fields = {
          { Field_to_Mask = string_array },
          { Masking_Chars = string_array },
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
      }
    }
  }
}
