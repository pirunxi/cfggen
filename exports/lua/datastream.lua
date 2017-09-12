 local tonumber = tonumber
 local sub = string.sub
 local gsub = string.gsub
 local gmatch = string.gmatch
 local lower = string.lower
 local error = error
 local open = io.open
 local setmetatable = setmetatable
 local tostring = tostring
 local insert = table.insert
 
 local magicstringfornewline = gsub(".g9~/", "%.", "%%%.")
 
 local os = {}
 os.__index = os
 
 function os.new(datafile)
    local o = {}
    setmetatable(o, os)
	o.data_iter = io.lines(datafile)
    return o
 end
 
 function os:dump()
    while true do
        local w = self:get_next()
        if not w then break end
        --print(w)
    end
 end
 
 function os:close()
    while self.data_iter() do
    end
 end
 
 function os:get_next()
    return self.data_iter()
 end
 
function os:get_bool()
    local next = lower(self:get_next())
    if next == "true" then
        return true
    elseif next == "false" then
        return false
    else
        error(tostring(next) .. "isn't bool")
    end
 end
 
function os:get_int()
    local next = self:get_next()
    return tonumber(next)
 end
 
function os:get_long() 
    local next = self:get_next()
    return tonumber(next)
 end 
 
function os:get_float()
    local next = self:get_next()
    return tonumber(next)
 end
 
function os:get_string() 
	local next = self:get_next()
	local s = gsub(next, magicstringfornewline, "\n")
	return s
end 
function os:get_list(key)
    local r = {}
    local oper = self["get_" .. key]
    for i = 1, self:get_int() do
          insert(r, oper(self))
    end
    return r
 end 
 
function os:get_set(key)
    local r = {}
    local oper = self["get_" .. key]
    for i = 1, self:get_int() do
        r[oper(self)] = true
    end
    return r
 end 
 
function os:get_map(key, value)
    local r = {}
    local oper_key = self["get_" .. key]
    local oper_value = self["get_" .. value]
    for i = 1, self:get_int() do
        r[oper_key(self)] = oper_value(self)
    end
    return r
 end
 
 return os