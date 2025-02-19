local cjson = require "cjson"

local DataMaskingHandler = {
  PRIORITY = 800,
  VERSION = "1.0"
}

-- Fungsi untuk memisahkan string berdasarkan separator
local function split(str, sep)
  local fields = {}
  local pattern = string.format("([^%s]+)", sep)
  str:gsub(pattern, function(c) fields[#fields + 1] = c end)
  return fields
end

-- Fungsi untuk melakukan masking pada nilai
local function mask_value(value, mask_char, expose_chars, pattern)
  if type(value) ~= "string" and type(value) ~= "number" then
    return value
  end

  value = tostring(value)

  -- Validasi pola jika disediakan
  if pattern and pattern ~= "" and not string.match(value, pattern) then
    return value
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

  -- Akumulasi setiap potongan data (chunk)
  ctx.buffered = (ctx.buffered or "") .. (chunk or "")

  if not eof then
    -- Jangan mengirimkan chunk apapun sebelum akhir data
    ngx.arg[1] = nil
    return
  end

  -- Cek apakah response merupakan JSON berdasarkan header Content-Type
  local content_type = kong.response.get_header("Content-Type")
  if not content_type or not content_type:find("application/json", 1, true) then
    -- Jika bukan JSON, kirimkan kembali data asli yang telah terkumpul
    ngx.arg[1] = ctx.buffered
    return
  end

  -- Parsing body JSON
  local success, parsed_body = pcall(cjson.decode, ctx.buffered)
  if not success then
    kong.log.err("Gagal melakukan parsing JSON body: ", parsed_body)
    ngx.arg[1] = ctx.buffered
    return
  end

  -- Memecah konfigurasi string menjadi tabel
  local field_names = split(conf.field_names, ",")
  local mask_chars = split(conf.mask_chars, ",")
  local expose_chars = split(conf.expose_chars, ",")
  local patterns = split(conf.patterns or "", ";")

  -- Melakukan masking untuk masing-masing field
  for i, field_name in ipairs(field_names) do
    field_name = field_name:match("^%s*(.-)%s*$")  -- Hilangkan spasi awal dan akhir
    if parsed_body[field_name] then
      parsed_body[field_name] = mask_value(
        parsed_body[field_name],
        mask_chars[i] or "x",
        expose_chars[i] or "0",
        patterns[i] or ""
      )
    end
  end

  -- Encode kembali body yang telah dimodifikasi ke format JSON
  local modified_body = cjson.encode(parsed_body)
  ngx.arg[1] = modified_body
end

return DataMaskingHandler