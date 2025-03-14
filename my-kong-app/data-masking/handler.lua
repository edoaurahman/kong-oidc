--handler.lua
local cjson = require "cjson"

local DataMaskingHandler = {
  VERSION = "1.0.0",
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
  if type(value) ~= "string" and type(value) ~= "number" then
    return value
  end

  value = tostring(value)
  
  -- Validasi pola jika disediakan
  if pattern and pattern ~= "" and not string.match(value, pattern) then
    kong.log.debug("Value doesn't match pattern, skipping mask: ", value)
    return value
  end

  -- Jika value berupa angka (hanya digit dengan spasi opsional), mask penuh tanpa expose
  if string.match(value, "^%s*%d+%s*$") then
    return string.rep(mask_char, #value)
  end

  local length = #value
  local expose_num = tonumber(expose_chars) or 0
  local exposed, masked = "", ""

  if expose_num > 0 and expose_num < length then
    exposed = string.sub(value, 1, expose_num)
    masked = string.rep(mask_char, length - expose_num)
  else
    masked = string.rep(mask_char, length)
  end

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

  -- Split konfigurasi
  local field_names = split(conf.field_names, ",")
  local mask_chars = split(conf.mask_chars, ",")
  local expose_chars = split(conf.expose_chars, ",")
  local patterns = split(conf.patterns or "", ";")

  kong.log.debug("Masking configuration: ", cjson.encode({
    field_names = field_names,
    mask_chars = mask_chars,
    expose_chars = expose_chars,
    patterns = patterns,
  }))

  local function process_object(obj)
    for i, field_path in ipairs(field_names) do
      field_path = field_path:match("^%s*(.-)%s*$") -- Hapus spasi di awal dan akhir
      local original_value = get_nested_value(obj, field_path)
      if original_value ~= nil then
        local masked_value = nil
        if type(original_value) == "table" then
          -- Jika field adalah array, iterasi setiap elemen
          masked_value = {}
          for idx, element in ipairs(original_value) do
            if type(element) == "string" or type(element) == "number" then
              masked_value[idx] = mask_value(
                element,
                mask_chars[i] or "x",
                expose_chars[i] or "0",
                patterns[i] or ""
              )
            else
              masked_value[idx] = element
            end
          end
        else
          masked_value = mask_value(
            original_value,
            mask_chars[i] or "x",
            expose_chars[i] or "0",
            patterns[i] or ""
          )
        end
        set_nested_value(obj, field_path, masked_value)
        kong.log.debug(string.format("Masked field: %s | Original: %s | Masked: %s",
          field_path, cjson.encode(original_value), cjson.encode(masked_value)))
      else
        kong.log.debug("Field not found: ", field_path)
      end
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
