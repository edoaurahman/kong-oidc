local cjson = require "cjson"

local DataMaskingHandler = {
  VERSION = "1.1.0",
  PRIORITY = 800,
}

-- Fungsi untuk memisahkan string berdasarkan separator
local function split(str, sep)
  if not str then
    return {}
  end

  local fields = {}

  -- Jika separator adalah titik, escape dengan benar
  local escape_sep = (sep == ".") and "%." or sep
  local pattern = string.format("([^%s]+)", escape_sep)
  str:gsub(pattern, function(c)
    fields[#fields + 1] = c
  end)
  return fields
end

-- Fungsi untuk mengambil nilai nested menggunakan notasi titik
local function get_nested_value(obj, path)
  local keys = split(path, ".")
  local current = obj
  for _, key in ipairs(keys) do
    if type(current) ~= "table" then
      return nil
    end
    current = current[key]
    if current == nil then
      return nil
    end
  end
  return current
end

-- Fungsi untuk mengatur nilai nested menggunakan notasi titik
local function set_nested_value(obj, path, value)
  local keys = split(path, ".")
  local current = obj
  local last_key = keys[#keys]
  for i = 1, #keys - 1 do
    local key = keys[i]
    if type(current[key]) ~= "table" then
      current[key] = {}
    end
    current = current[key]
  end
  current[last_key] = value
end

-- Fungsi untuk melakukan masking pada nilai
local function mask_value(value, mask_char, expose_chars, pattern)
  -- Set default values if passed parameter is nil
  mask_char = mask_char or "x"
  expose_chars = tonumber(expose_chars) or 0
  pattern = pattern or ""

  local exposed, masked = "", ""
 
  if type(value) ~= "string" and type(value) ~= "number" then
    return value
  end

  value = tostring(value)
  
  -- Validasi pola jika disediakan
  -- if pattern and pattern ~= "" and not string.match(value, pattern) then
  --   kong.log("Ini coba log:", pattern)
  --   kong.log("Value doesn't match pattern, skipping mask: ", value)
  --   return value
  -- end

  -- Jika value berupa angka (hanya digit dengan spasi opsional), mask penuh tanpa expose
  -- if string.match(value, "^%s*%d+%s*$") then
  --   return string.rep(mask_char, #value)
  -- end

  if expose_chars > 0 and expose_chars < #value then
    exposed = string.sub(value, 1, expose_chars)
    masked = string.rep(mask_char, #value - expose_chars)
  else
    masked = string.rep(mask_char, #value)
  end

  -- Return finished
  return exposed .. masked
end

-- Fungsi body_filter untuk mengumpulkan dan memodifikasi body respon
function DataMaskingHandler:body_filter(conf)
  local ctx = kong.ctx.plugin
  local chunk = ngx.arg[1]
  local eof = ngx.arg[2]

  -- Akumulasi potongan data (chunk)
  ctx.buffered = (ctx.buffered or "") .. (chunk or "")

  if not eof then
    kong.log.debug("Waiting for more chunks. Current buffer: ", ctx.buffered)
    ngx.arg[1] = nil  -- Jangan kirim data sebelum EOF
    return
  end

  kong.log.debug("Complete response received: ", ctx.buffered)

  -- Cek apakah response bertipe JSON
  local content_type = kong.response.get_header("Content-Type")
  if not content_type or not content_type:find("application/json", 1, true) then
    kong.log.debug("Non-JSON content type: ", content_type)
    ngx.arg[1] = ctx.buffered
    return
  end

  -- Parsing body JSON
  local success, parsed_body = pcall(cjson.decode, ctx.buffered)
  if not success then
    kong.log.err("Failed to parse JSON body: ", parsed_body)
    ngx.arg[1] = ctx.buffered
    return
  end

  -- Create variables for configuration from user input
  local Field_to_Mask = conf.Field_to_Mask
  local Masking_Chars = conf.Masking_Chars
  local expose_chars = split(conf.expose_chars, ",")
  local patterns = split(conf.patterns or "", ";")

  -- Log for appliied configurations
  kong.log.debug("Applied configuration: ", cjson.encode({
    Field_to_Mask = Field_to_Mask,
    Masking_Chars = Masking_Chars,
    expose_chars = expose_chars,
    patterns = patterns,
  }))

  local function process_object(obj)
    for i = 1, #Field_to_Mask do
      -- Remove whitespaces
      Field_to_Mask[i] = Field_to_Mask[i]:match("^%s*(.-)%s*$")

      local original_value = get_nested_value(obj, Field_to_Mask[i])

      if original_value == nil then
        kong.log.debug("Field not found: ", Field_to_Mask[i])
        return
      end

      local masked_value = nil

      -- Jika field adalah array, iterasi setiap elemen
      if type(original_value) == "table" then
        masked_value = {}
        for idx, element in ipairs(original_value) do
          if type(element) == "string" or type(element) == "number" then
            masked_value[idx] = mask_value(
              element,
              Masking_Chars[i],
              expose_chars[i],
              patterns[i]
            )
          else
            masked_value[idx] = element
          end
        end
      else
        masked_value = mask_value(
          original_value,
          Masking_Chars[i],
          expose_chars[i],
          patterns[i]
        )
      end

      set_nested_value(obj, Field_to_Mask[i], masked_value)
      
      kong.log.debug(string.format("Masked field: %s | Original: %s | Masked: %s",
      Field_to_Mask[i], cjson.encode(original_value), cjson.encode(masked_value)))
    end
  end

  -- Jika parsed_body adalah array, iterasi setiap objek
  if type(parsed_body) == "table" and #parsed_body > 0 then
    for _, item in ipairs(parsed_body) do
      process_object(item)
    end
  else
    -- Jika bukan array, proses sebagai objek tunggal
    process_object(parsed_body)
  end

  local modified_body = cjson.encode(parsed_body)
  kong.log.debug("Modified response: ", modified_body)
  ngx.arg[1] = modified_body
end

return DataMaskingHandler
