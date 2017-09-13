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
	
	String genStructBody(ArrayList<Field> fields, ArrayList<String> ls) {
		StringBuilder sb = new StringBuilder();
		for(Field f : fields) {
			String ftype = f.getType();
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
							for(String idx : f.getIndexs()) {
								sb.append("{},");
							}
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
		ls.add("local function global_var(namespace) local t = _G local idx = 1 while true do local start, ends = find(namespace, '.', idx, true) local subname = sub(namespace, idx, start and start - 1) local subt = t[subname] if not subt then\n" +
				"\tsubt = {} t[subname] = subt end t = subt if start then idx = ends + 1 else return t end end end\n");


		for(Struct struct : Struct.getExports()) {
			final String fullname = struct.getFullName();
			final String name = struct.getName();
			ls.add("do");
			ls.add("local meta = {}");

			ArrayList<Field> fields = struct.getSelfAndParentFiels();
			StringBuilder sb = new StringBuilder("local _indexs = {class=0,");
			int index = 0;
			for(Field f : fields) {
				sb.append(f.getName()).append('=').append(++index).append(',');
				for(String idx : f.getIndexs()) {
					sb.append(f.getName()).append('_').append(idx).append('=').append(++index).append(',');
				}
			}
			sb.append('}');
			ls.add(sb.toString());
			ls.add("meta.__index = function (t, key) local index = _indexs[key] return rawget(t, index) or (index == 0 and '" + name + "') or nil end");
			for(Const c : struct.getConsts()) {
				ls.add(String.format("meta.%s = %s", c.getName(), toLuaValue(c.getType(), c.getValue())));
			}
			//ls.add(String.format("global_var('%s')['%s'] = meta", struct.getNamespace(), name));
			
			ls.add(String.format("os['%s'] = function (self)", fullname));
			if(struct.isDynamic()) {
				ls.add("return self[string(self)](self)");
			} else {


				ls.add("local o = {" + genStructBody(fields, ls) + "}");
				ls.add("setmetatable(o, meta)");
				for(Field f : fields) {
					String fname = f.getName();
					if (!f.getIndexs().isEmpty()) {
						ls.add("do");
						for (String idx : f.getIndexs()) {
							ls.add(String.format("local _%s = o.%s_%s", idx, fname, idx));
						}
						ls.add(String.format("for _, _V in ipairs(o.%s) do", fname));
						for (String idx : f.getIndexs()) {
							ls.add(String.format("_%s[_V.%s] = _V", idx, idx));
						}
						ls.add("end");
						ls.add("end");
					}
				}

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
		ls.add("local cfgs = {}");
		ls.add("for _, s in ipairs({");
		exportConfigs.forEach(c -> ls.add(String.format("{name='%s', type='%s', index='%s', output='%s', single=%s},",
			c.getName(), c.getType(), c.isSingle() ? "" : c.getIndex(), c.getOutputDataFile(), c.isSingle())));
		ls.add("}) do");
		

		ls.add("local fs = os.create(s.output)");
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
