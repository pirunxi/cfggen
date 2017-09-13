package configgen.lans.lua;

import configgen.Generator;
import configgen.Main;
import configgen.Utils;
import configgen.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeGen implements Generator {
	@Override
	public void gen() {	
		genStructAndEnums();		
		genConfig();
	}

	private String toLuaType(String type) {
		return ENUM.isEnum(type) ? "int" : type;
	}
	
	String genStructBody(Struct struct, ArrayList<String> ls) {
		StringBuilder sb = new StringBuilder();
		for(Field f : struct.getSelfAndParentFiels()) {
			String ftype = f.getType();
			String fname = f.getName();
			final List<String> ftypes = f.getTypes();
			if(f.checkInGroup(Main.groups)) {
				if(f.isRaw()) {
					sb.append(String.format("%s(self),", ftype));
				} else if(f.isStruct()) {
					sb.append(String.format("os['%s'](self),", ftype));
				} else if(f.isEnum()) {
					sb.append("int(self),");
				} else if(f.isContainer()) {
					switch(ftype) {
						case "list": {
							final String valueType = toLuaType(ftypes.get(1));
							sb.append(String.format("list(self, '%s'),", valueType));
							
							break;
						}
						case "set": {
							final String valueType = toLuaType(ftypes.get(1));
							sb.append(String.format("set(self, '%s'),", valueType));
							break;
						}
						case "map": {
							final String keyType = toLuaType(ftypes.get(1));
							final String valueType = toLuaType(ftypes.get(2));
							sb.append(String.format("map(self, '%s', '%s'),", keyType, valueType));
							break;
						}
					}
				} else {
					Utils.error("unknown type:" + ftype);
				}
			}
		}
		return sb.toString();
	}
	
	String toLuaValue(String type, String value) {
		switch(type) {
		case "string": return "\"" + value + "\"";
		case "list:int":
		case "list:float": return "{" + value + "}";
		default: return value;
		}

	}
	
	void genStructAndEnums() {
		final ArrayList<String> ls = new ArrayList<String>();
		final String namespace = "cfg";
		ls.add(String.format("local os = require '%s.datastream'", namespace));

		ls.add("local insert = table.insert");
		ls.add("local ipairs = ipairs");
		ls.add("local setmetatable = setmetatable");
		ls.add("local find = string.find");
		ls.add("local sub = string.sub");
		ls.add("local rawget = rawget");
		ls.add("local bool = os.bool\n" +
				"local int = os.int\n" +
				"local long = os.long\n" +
				"local string = os.string\n" +
				"local float = os.float\n" +
				"local list = os.list\n" +
				"local set = os.set\n" +
				"local map = os.map");
		ls.add("local function global_var(namespace)");
		ls.add("local t = _G");
		ls.add("local idx = 1");
		ls.add("while true do");
		ls.add("local start, ends = find(namespace, '.', idx, true)");
		ls.add("local subname = sub(namespace, idx, start and start - 1)");
		ls.add("local subt = t[subname]");
		ls.add("if not subt then");
		ls.add("subt = {}");
		ls.add("      t[subname] = subt");
		ls.add("end");
		ls.add("    t = subt");
		ls.add("if start then"); 
		ls.add("idx = ends + 1");
		ls.add("else"); 
		ls.add("return t");
		ls.add("end");
		ls.add("end");
		ls.add("end");

		ls.add("function os:gettype(typename)");
		ls.add("return self[typename](self)");
		ls.add("end");
		ls.add("");

		for(Struct struct : Struct.getExports()) {
			final String fullname = struct.getFullName();
			final String name = struct.getName();
			ls.add("do");
			ls.add("local meta = {}");

			StringBuilder sb = new StringBuilder("local indexs = {class=0,");
			int index = 0;
			for(Field f : struct.getSelfAndParentFiels()) {
				sb.append(f.getName()).append('=').append(++index).append(',');
			}
			sb.append('}');
			ls.add(sb.toString());
			ls.add("meta.__index = function (t, key) local index = indexs[key] return rawget(t, index) or (index == 0 and '" + name + "') or nil end");
			for(Const c : struct.getConsts()) {
				ls.add(String.format("meta.%s = %s", c.getName(), toLuaValue(c.getType(), c.getValue())));
			}
			//ls.add(String.format("global_var('%s')['%s'] = meta", struct.getNamespace(), name));
			
			ls.add(String.format("os['%s'] = function (self)", fullname));
			if(struct.isDynamic()) {
				ls.add("return self[string(self)](self)");
			} else {
				ls.add("local o = {" + genStructBody(struct, ls) + "}");
				ls.add("setmetatable(o, meta)");
				ls.add("return o");
			}
			ls.add("end");
			ls.add("end");
		}
		
		for(ENUM e : ENUM.getExports()) {
			final String name = e.getName();
			ls.add(String.format("global_var('%s')['%s'] = {", e.getNamespace(), name));
			for(Map.Entry<String, Integer> me : e.getCases().entrySet()) {
				ls.add(String.format("%s = %d,", me.getKey(), me.getValue()));
			}
			ls.add("}");
		}


	
		ls.add("return os");
		final String code = ls.stream().collect(Collectors.joining("\n"));
		//Main.println(code);
		final String outFile = String.format("%s/%s/structs.lua", Main.codeDir, namespace.replace('.', '/'));
		Utils.save(outFile, code);
	}
	
	void genConfig() {
		final List<Config> exportConfigs = Config.getExportConfigs();
		final String namespace = "cfg";
		final ArrayList<String> ls = new ArrayList<String>();
		ls.add(String.format("local os = require '%s.structs'", namespace));
		ls.add("local create_datastream = create_datastream");
		ls.add("local cfgs = {}");
		ls.add("for _, s in ipairs({");
		exportConfigs.forEach(c -> ls.add(String.format("{name='%s', type='%s', index='%s', output='%s', single=%s},",
			c.getName(), c.getType(), c.isSingle() ? "" : c.getIndex(), c.getOutputDataFile(), c.isSingle())));
		ls.add("}) do");
		

		ls.add(String.format("local fs = create_datastream(s.output)"));
		ls.add("local method = os[s.type]");
		ls.add("if not s.single then");
		ls.add("local c = {}");
		ls.add("for i = 1, fs:int() do");
		ls.add("local v = method(fs)");
		ls.add("c[v[s.index]] = v");
		ls.add("end");
		ls.add("cfgs[s.name] = c");
		ls.add("else");
		ls.add("if fs:int() ~= 1 then error('single config size != 1') end");
		ls.add("cfgs[s.name] = method(fs)");
		ls.add("end");
        ls.add("fs:close()");
		ls.add("end");
		
		ls.add("return cfgs");
		
		final String outFile = String.format("%s/%s/configs.lua", Main.codeDir, namespace.replace('.', '/'));
		final String code = ls.stream().collect(Collectors.joining("\n"));
		Utils.save(outFile, code);
	}

}
