local os = require "cfg.structs"

local insert = table.insert
local concat = table.concat
local tostring = tostring

local function dump_atom (x)
    return tostring(x)
end

local function dump_table(t)
  local code = {"{"}
  
  for k, v in pairs(t) do
    if type(v) ~= "table" then
      insert(code, tostring(k) .. "=" .. dump_atom(v) .. ",")
    else
      insert(code, tostring(k) .. "=" .. dump_table(v) .. ",")
    end
  end
 insert(code, "}")
 return concat(code)
end


local os = require "cfg.structs"

function create_datastream(file)
    return os.new("../data/" .. file)
end

local cfgs = require "cfg.configs"

print(dump_table(cfgs))
