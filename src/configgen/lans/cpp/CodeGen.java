package configgen.lans.cpp;

import configgen.Generator;
import configgen.Main;
import configgen.Utils;
import configgen.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CodeGen implements Generator {
	@Override
	public void gen() {
		genStructs();
		genConfigs();
		genAllDefines();
		genStub();
	}

	private void save(List<String> lines, String file) {
		final String code = lines.stream().collect(Collectors.joining("\n"));
		final String outFile = String.format("%s/%s", Main.codeDir, file.toLowerCase());
		Utils.save(outFile, code);
	}

	private final static String ENUM_TYPE = "int32_t";
	
	private String readType(String type) {
		switch(type) {
			case "bool": return "fs.getBool()";
			case "int": return "fs.getInt()";
			case "long": return "fs.getLong()";
			case "float":  return "fs.getFloat()";
			case "string": return "fs.getString()";
			default: {
				if(ENUM.isEnum(type)) {
					return "fs.getInt()";
				}
				final String cppType = toCppType(type);
				return Struct.isDynamic(type) ?
					String.format("(%s*)fs.getObject(fs.getString())", cppType) :
					String.format("new %s(fs)", cppType);
			}
		}
	}

	private String toCppType(String type) {
		if(Struct.isStruct(type)) {
			return type.replace(".", "::");
		}
		if(ENUM.isEnum(type)) {
			return ENUM_TYPE;
		}
		switch(type) {
			case "bool" :
			case "float": return type;
			case "string" : return "std::string";
			case "int" : return "int32_t";
			case "long" : return "int64_t";
			case "list" : return "std::vector";
			case "set" : return "std::set";
			case "map" : return "std::map";
		}
		throw new RuntimeException("unknown type:" + type);
	}

	private String toCppDefineType(String type) {
		return (Struct.isStruct(type)) ? type.replace(".", "::") + "*" : toCppType(type);
	}
	
	private String toCppValue(String type, String value) {
		switch(type) {
		case "string": return "\"" + value + "\"";
		default: return value;
		}
	}

	private String getDefMacroBegin(String fullName) {
		final String macro = "__" + fullName.replace('.', '_').toUpperCase() + "__H__";
		return "#ifndef " + macro + "\n"
				+"#define " + macro + "\n";
	}

	private String getDefMacroEnd() {
		return "#endif";
	}

	private String getDefNamespaceBegin(String namespace) {
		final StringBuilder sb = new StringBuilder();
		for(String n : namespace.split("\\.")) {
			sb.append("namespace ").append(n).append("{");
		}
		return sb.toString();
	}

	private String getDefNamespaceEnd(String namespace) {
		final StringBuilder sb = new StringBuilder();
		for(String ignored : namespace.split("\\.")) {
			sb.append("}");
		}
		return sb.toString();
	}

	private void includeAlldefines(List<String> ls) {
		include(ls, "alldefines");
	}

	private void genStructs() {
		final List<Struct> structs = Struct.getExports();
		structs.forEach(this::genStructHeadFile);
		for(int i = 0, n = (structs.size() + CLASS_PER_CONFIG - 1) / CLASS_PER_CONFIG ; i < n ; i++) {
			genStructCppFile(structs.subList(i * CLASS_PER_CONFIG, Math.min(structs.size(), (i + 1) * CLASS_PER_CONFIG)), i);
		}
	}
	
	private void genStructHeadFile(Struct struct) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = struct.getNamespace();
		final String fullName = struct.getFullName();
		ls.add(getDefMacroBegin(fullName));
		includeAlldefines(ls);
		
		final String base = struct.getBase();
		final String name = struct.getName();
		final boolean isDynamic = struct.isDynamic() ;

		if(!base.isEmpty()) {
			include(ls, base);
		}

		ls.add(getDefNamespaceBegin(namespace));

		ls.add(String.format("class %s : public %s {", name, (base.isEmpty() ? "cfg::Object" : toCppType(base))));
		ls.add("public:");
		
		if(!isDynamic) {
			ls.add("static int TYPEID;");
			ls.add("int getTypeId() { return TYPEID; }");
		}
		
		for(Const c : struct.getConsts()) {
			final String ctype = c.getType();
			final String cname = c.getName();
			if(Field.isRaw(ctype)) {
				final String cppType = toCppType(ctype);
				ls.add(String.format("static %s %s;", cppType, cname));
			} else {
				switch(ctype) {
					case "list:int" :
						ls.add(String.format("static int32_t %s[];", cname));
						break;
					case "list:float" :
						ls.add(String.format("static double %s[];", cname));
						break;
					default:
						Utils.error("struct:%s const:%s unknown type:%s", fullName, cname, ctype);
				}
			}
		}
		
		for(Field f : struct.getFields()) {
			final String ftype = f.getType();
			final String fname = f.getName();
			final List<String> ftypes = f.getTypes();
			if(f.checkInGroup(Main.groups)) {
				if(f.isRawOrEnumOrStruct()) {
					final String dtype = toCppDefineType(ftype);
					ls.add(String.format("%s %s;", dtype, fname));
				} else if(f.isContainer()) {
					switch(ftype) {
						case "list": {
							final String vtype = ftypes.get(1);
							final String dvtype = toCppDefineType(vtype);
							ls.add(String.format("std::vector<%s> %s;", dvtype, fname));
							if(!f.getIndexs().isEmpty()) {
								final Struct s = Struct.get(vtype);
								ls.addAll(f.getIndexs().stream().map(idxName -> String.format("std::map<%s, %s> %s_%s;",
										toCppDefineType(s.getField(idxName).getType()), dvtype, fname, idxName)).collect(Collectors.toList()));
							}
							break;
						}
						case "set": {
							ls.add(String.format("std::set<%s> %s;", toCppDefineType(ftypes.get(1)), fname));
							break;
						}
						case "map": {
							ls.add(String.format("std::map<%s, %s> %s;", toCppDefineType(ftypes.get(1)), toCppDefineType(ftypes.get(2)), fname));
							break;
						}
						default:
						    throw new RuntimeException("unknown field type:" + ftype);
					}
				} else {
					Utils.error("unknown type:" + ftype);
				}
			}
		}

		ls.add(String.format("%s(cfg::DataStream& fs);", name));
		ls.add("};");

		ls.add(getDefNamespaceEnd(namespace));
		ls.add(getDefMacroEnd());
		save(ls, fullName + ".h");
	}

	private void genStructCppFile(List<Struct> structs, int id) {
		final ArrayList<String> ls = new ArrayList<>();
		for(Struct struct : structs) {
			final String namespace = struct.getNamespace();
			final String fullName = struct.getFullName();

			final String name = struct.getName();
			final String base = struct.getBase();

			include(ls, fullName);
			struct.getRefStructs().forEach(s -> include(ls, s));

			ls.add(getDefNamespaceBegin(namespace));
			if (!struct.isDynamic()) {
				ls.add(String.format("int %s::TYPEID = %s;", name, struct.getTypeId()));
			}

			for (Const c : struct.getConsts()) {
				final String cType = c.getType();
				final String value = c.getValue();
				final String cname = c.getName();
				if (Field.isRaw(cType)) {
					final String cppType = toCppType(cType);
					ls.add(String.format("%s %s::%s = %s;", cppType, name, cname, toCppValue(cType, value)));
				} else {
					switch (cType) {
						case "list:int":
							ls.add(String.format("int32_t %s::%s[] = {%s};", name, cname, value));
							break;
						case "list:float":
							ls.add(String.format("double %s::%s[] = {%s};", name, cname, value));
							break;
						default:
							Utils.error("struct:%s const:%s unknown type:%s", fullName, cname, cType);
					}
				}
			}

			ls.add(String.format("%s::%s(cfg::DataStream& fs) %s {", name, name,
					(base.isEmpty() ? "" : String.format(": %s(fs)", Struct.get(base).getName()))));

			for (Field f : struct.getFields()) {
				String ftype = f.getType();
				final String fname = f.getName();
				final List<String> ftypes = f.getTypes();
				if (f.checkInGroup(Main.groups)) {
					if (f.isRawOrEnumOrStruct()) {
						ls.add(String.format("%s = %s;", fname, readType(ftype)));
					} else if (f.isContainer()) {
						switch (ftype) {
							case "list": {
								final String vtype = ftypes.get(1);
								final String dvtype = toCppDefineType(vtype);
								ls.add("for(int n = fs.getSize(); n-- > 0 ; ) {");
								if (!f.getIndexs().isEmpty()) {
									ls.add(String.format("%s _x = %s;", dvtype, readType(vtype)));
									ls.add(String.format("%s.push_back(_x);", fname));
									ls.addAll(f.getIndexs().stream().map(idx -> String.format("%s_%s[_x->%s] = _x;", fname, idx, idx))
											.collect(Collectors.toList()));
								} else {
									ls.add(String.format("%s.push_back(%s);", fname, readType(vtype)));
								}
								ls.add("}");
								break;
							}
							case "set": {
								ls.add("for(int n = fs.getSize(); n-- > 0 ; ) {");
								ls.add(String.format("%s.insert(%s);", fname, readType(ftypes.get(1))));
								ls.add("}");
								break;
							}
							case "map": {
								final String ktype = ftypes.get(1);
								final String vtype = ftypes.get(2);
								ls.add("for(int n = fs.getSize(); n-- > 0 ; ) {");
								ls.add(String.format("%s _key = %s;", toCppDefineType(ktype), readType(ktype)));
								ls.add(String.format("%s[_key] = %s;", fname, readType(vtype)));
								ls.add("}");
								break;
							}
						}
					} else {
						Utils.error("unknown type:" + ftype);
					}
				}
			}
			ls.add("}");

			ls.add(getDefNamespaceEnd(namespace));

		}

		save(ls, "structs" + id + ".cpp");
	}

	private void include(List<String> ls, String header) {
		ls.add("#include \"" + header.toLowerCase() + ".h\"");
	}

	private final static int CLASS_PER_CONFIG = 100;
	private void genConfigs() {
		final ArrayList<String> ls = new ArrayList<>();
		List<Config> configs = Config.getExportConfigs();
		final String namespace = "cfg";
		final String name = Main.cfgmgrName;
		final String fullName = namespace + "." + name;
		ls.add(getDefMacroBegin(fullName));
		includeAlldefines(ls);
		ls.add(getDefNamespaceBegin(namespace));

		ls.add(String.format("class %s {", name));
		ls.add("public:");
		ls.add(String.format("%s& getInstance() { static %s instance; return instance; }", name, name));
		configs.forEach(c -> {
			final String dvtype = toCppDefineType(c.getType());
			final String cname = c.getName();
			if(!c.isSingle()) {
				ls.add(String.format("std::map<%s, %s> %s;", toCppDefineType(c.getIndexType()), dvtype, cname));
			} else {
				ls.add(String.format("%s %s;", dvtype, cname));
			}
			}
		);
		for(int i = 0, n = (configs.size() + CLASS_PER_CONFIG - 1) / CLASS_PER_CONFIG ; i < n ; i++) {
			ls.add(String.format("void load%s(const std::string& dataDir);", i));
		}
		ls.add("void load(const std::string& dataDir) {");
		for(int i = 0, n = (configs.size() + CLASS_PER_CONFIG - 1) / CLASS_PER_CONFIG ; i < n ; i++) {
			ls.add(String.format("load%s(dataDir);", i));
			genSubConfig(configs.subList(i * CLASS_PER_CONFIG, Math.min((i+1) * CLASS_PER_CONFIG, configs.size())), i);
		}
		ls.add("}");
		ls.add("};");
		ls.add(getDefNamespaceEnd(namespace));
		ls.add(getDefMacroEnd());
		save(ls, name + ".h");
	}

	private void genSubConfig(List<Config> configs, int id) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = "cfg";
		final String name = Main.cfgmgrName;
		include(ls, name);
		configs.forEach(c -> include(ls, c.getType()));
		ls.add(getDefNamespaceBegin(namespace));

		ls.add(String.format("void CfgMgr::load%s(const std::string& dataDir) {", id));
		configs.forEach(
				c -> {
					ls.add("{");
					ls.add(String.format("cfg::DataStream& fs = *cfg::DataStream::create(dataDir + \"/\" + \"%s\");", c.getOutputDataFile()));
					final String cname = c.getName();
					final String vtype = c.getType();
					final String vdtype = toCppDefineType(vtype);
					if(!c.isSingle()) {
						ls.add("for(int n = fs.getSize() ; n-- > 0 ; ) {");
						ls.add(String.format("%s v = %s;", vdtype, readType(vtype)));
						ls.add(String.format("this->%s[v->%s] = v;", cname, c.getIndex()));
						ls.add("}");
						ls.add(String.format("std::cout << \"%s\" << \",size=\" << this->%s.size() << std::endl;", cname, cname));
					} else {
						ls.add("if(fs.getSize() != 1) throw cfg::Error(\"%s\", \"single config but size != 1\");");
						ls.add(String.format("this->%s = %s;", cname, readType(vtype)));
					}
					ls.add("}");
				});
		ls.add("}");

		ls.add(getDefNamespaceEnd(namespace));
		save(ls, name + id + ".cpp");
	}

	private void genAllDefines() {
		final ArrayList<String> ls = new ArrayList<>();
		final String name = "alldefines";
		ls.add(getDefMacroBegin(name));
		include(ls, "datastream");
		include(ls, "object");

		Struct.getExports().forEach(s -> {
			ls.add(getDefNamespaceBegin(s.getNamespace()));
			ls.add("class " + s.getName() + ";");
			ls.add(getDefNamespaceEnd(s.getNamespace()));
		});
		ENUM.getExports().forEach(e -> {
			final String namespace = e.getNamespace();
			ls.add(getDefNamespaceBegin(namespace));
			ls.add(String.format("class %s {enum {", e.getName()));
			ls.addAll(e.getCases().entrySet().stream().map(me -> String.format("%s = %s,", me.getKey().replace("NULL", "null"), me.getValue()))
					.collect(Collectors.toList()));
			ls.add("};};");
			ls.add(getDefNamespaceEnd(namespace));
		});


		ls.add(getDefMacroEnd());
		save(ls, name + ".h");
	}

	private void genStub() {
		final ArrayList<String> ls = new ArrayList<>();
		List<Struct> exports = Struct.getExports();

		final String namespace = "cfg";
		final String name = "Stub";
		final String fullName = namespace + "." + name;
		ls.add(getDefMacroBegin(fullName));
		includeAlldefines(ls);

		exports.stream().filter(s -> !s.isDynamic()).forEach(s -> include(ls, s.getFullName()));

		ls.add(getDefNamespaceBegin(namespace));

		ls.add(String.format("class %s {", name));
		ls.add(String.format("public: %s() {", name));
		exports.stream().filter(s -> !s.isDynamic()).forEach(s ->
				ls.add(String.format("DataStream::registerType<%s>(\"%s\");", toCppType(s.getFullName()), s.getFullName())));
		ls.add("}} stub;");

		ls.add(getDefNamespaceEnd(namespace));
		ls.add(getDefMacroEnd());

		save(ls, "stub.cpp");
	}

}
