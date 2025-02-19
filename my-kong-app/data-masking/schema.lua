-- schema.lua
local typedefs = require "kong.db.schema.typedefs"

return {
    name = "data-masking",
    fields = {{
        consumer = typedefs.no_consumer
    }, {
        protocols = typedefs.protocols_http
    }, {
        config = {
            type = "record",
            fields = {
                -- Field names (comma-separated)
                {
                    field_names = {
                        type = "string",
                        required = true,
                        default = "nik,nama",
                        description = "Comma-separated field names to mask"
                    }
                },
                -- Mask characters (comma-separated)
                {
                    mask_chars = {
                        type = "string",
                        required = true,
                        default = "x,x",
                        description = "Comma-separated mask characters for each field"
                    }
                },
                -- Exposed characters (comma-separated)
                {
                    expose_chars = {
                        type = "string",
                        required = true,
                        default = "2,1",
                        description = "Comma-separated numbers of characters to expose"
                    }
                },
                -- Patterns (semicolon-separated due to commas in patterns)
                {
                    patterns = {
                        type = "string",
                        required = false,
                        default = "^%d+$;^[A-Za-z%s]+$",
                        description = "Semicolon-separated patterns for validation"
                    }
                }
            }
        }
    }}
}