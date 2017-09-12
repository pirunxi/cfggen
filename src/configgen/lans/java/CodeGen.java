package configgen.lans.java;

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
		Struct.getExports().forEach(this::genStruct);
		ENUM.getExports().forEach(this::genEnum);
		genConfig();
	}

	private void save(String fullTypeName, ArrayList<String> lines) {
		final String code = lines.stream().collect(Collectors.joining("\n"));
		final String outFile = String.format("%s/%s.java", Main.codeDir, fullTypeName.replace('.', '/'));
		Utils.save(outFile, code);
	}

	private String readType(String type) {
		switch(type) {
			case "bool": return "fs.getBool()";
			case "int": return "fs.getInt()";
			case "long": return "fs.getLong()";
			case "float":  return "fs.getFloat()";
			case "string": return "fs.getString()";
			default: {
				if(ENUM.isEnum(type))
					return "fs.getInt()";
				if(Struct.isDynamic(type))
					return String.format("(%s)fs.getObject(fs.getString())", type);
				else
					return String.format("new %s(fs)", type);
			}
		}
	}
	
	private String toJavaValue(String type, String value) {
		switch(type) {
		case "string": return "\"" + value + "\"";
		case "float": return value + "f";
		default: return value;
		}
	}

	private String toJavaType(String type) {
		switch(type) {
			case "bool" : return "boolean";
			case "string" : return "String";
			default: return ENUM.isEnum(type) ? "int" : type;
		}
	}

	private String toBoxType(String type) {
		switch(type) {
			case "bool": return "Boolean";
			case "int": return "Integer";
			case "long": return "Long";
			case "float": return "Float";
			case "string": return "String";
			default : return ENUM.isEnum(type) ? "Integer" : type;
		}
	}

	private void genEnum(ENUM e) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = e.getNamespace();
		ls.add("package " + namespace + ";");
		final String name = e.getName();
		ls.add(String.format("public final class %s {", name));
		ls.addAll(e.getCases().entrySet().stream().map(me ->
				String.format("	public final static int %s = %d;", me.getKey(), me.getValue())).collect(Collectors.toList()));
		ls.add(String.format("	public final static java.util.List<Integer> enums = java.util.Arrays.asList(%s);",
				e.getCases().entrySet().stream().filter(ee -> !ee.getKey().equalsIgnoreCase("null"))
				.map(ee-> ee.getValue().toString()).collect(Collectors.joining(" ,"))));
		ls.add("}");
		save(e.getFullname(), ls);
	}

	private void genStruct(Struct struct) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = struct.getNamespace();
		ls.add("package " + namespace + ";");
		
		final String base = struct.getBase();
		final String name = struct.getName();
		final boolean isDynamic = struct.isDynamic() ;
		ls.add(String.format("public %s class %s extends %s {", (isDynamic ? "abstract" : "final"), name, (base.isEmpty() ? "cfg.CfgObject" : base)));
		
		if(!isDynamic) {
			ls.add(String.format("	public final static int TYPEID = %s;", struct.getTypeId()));
			ls.add("	final public int getTypeId() { return TYPEID; }");
		}
		
		for(Const c : struct.getConsts()) {
			final String type = c.getType();
			final String value = c.getValue();
			final String cname = c.getName();
			if(Field.isRaw(type)) {
				ls.add(String.format("	public static final %s %s = %s;",
						toJavaType(type), cname, toJavaValue(type, value)));
			} else {
				switch(type) {
					case "list:int" :
						ls.add(String.format("	public static final int[] %s = {%s};", cname, value));
						break;
					case "list:float" :
						ls.add(String.format("	public static final double[] %s = {%s};", cname, value));
						break;
					default:
						Utils.error("struct:%s const:%s unknown type:%s", struct.getFullName(), c.getName(), type);	
				}
			}
		}
		
		final ArrayList<String> ds = new ArrayList<>();
		final ArrayList<String> cs = new ArrayList<>();

		cs.add(String.format("	public %s(cfg.DataStream fs) {", name));
		if(!base.isEmpty()) {
			cs.add("		super(fs);");
		}
		
		for(Field f : struct.getFields()) {
			String ftype = f.getType();
			String jtype = toJavaType(ftype);
			final String fname = f.getName();
			final List<String> ftypes = f.getTypes();
			if(f.checkInGroup(Main.groups)) {
				if(f.isRawOrEnumOrStruct()) {
					ds.add(String.format("	public final %s %s;", jtype, fname));
					cs.add(String.format("		this.%s = %s;", fname, readType(ftype)));
				} else if(f.isContainer()) {
					switch(ftype) {
						case "list": {
							final String vtype = ftypes.get(1);
							final String jvtype = toJavaType(vtype);
							final String bvtype = toBoxType(ftypes.get(1));
							ds.add(String.format("	public final java.util.List<%s> %s = new java.util.ArrayList<>();", bvtype, fname));
							cs.add("		for(int n = fs.getInt(); n-- > 0 ; ) {");
							if(f.getIndexs().isEmpty()) {
								cs.add(String.format("			this.%s.add(%s);", fname, readType(vtype)));
							} else {
								cs.add(String.format("			final %s _v = %s;", jvtype, readType(vtype)));
								cs.add(String.format("			this.%s.add(_v);", fname));
								final Struct s = Struct.get(vtype);
								for(String idx : f.getIndexs()) {
									final String bktype = toBoxType(s.getField(idx).getType());
									final String idxFieldName = fname + "_" + idx;
									ds.add(String.format("	public final java.util.Map<%s, %s> %s= new java.util.HashMap<>();",
											bktype, bvtype, idxFieldName));
									cs.add(String.format("			this.%s.put(_v.%s, _v);", idxFieldName, idx));
								}
							}
							cs.add("		}");
							break;
						}
						case "set": {
							final String vtype = ftypes.get(1);
							ds.add(String.format("	public final java.util.Set<%s> %s = new java.util.HashSet<>();", toBoxType(vtype), fname));
							cs.add("		for(int n = fs.getInt(); n-- > 0 ; ) {");
							cs.add(String.format("			this.%s.add(%s);", fname, readType(vtype)));
							cs.add("		}");
							break;
						}
						case "map": {
							final String ktype = ftypes.get(1);
							final String vtype = ftypes.get(2);
							ds.add(String.format("	public final java.util.Map<%s, %s> %s = new java.util.HashMap<>();",
									toBoxType(ktype), toBoxType(vtype), fname));
							cs.add("		for(int n = fs.getInt(); n-- > 0 ; ) {");
							cs.add(String.format("			final %s _k = %s;", toJavaType(ktype), readType(ktype)));
							cs.add(String.format("			this.%s.put(_k, %s);", fname, readType(vtype)));
							cs.add("		}");
							break;
						}
					}
				} else {
					Utils.error("unknown type:" + ftype);
				}
			}
		}
		
		cs.add("	}");
		ls.addAll(ds);

		ls.addAll(cs);
		
		ls.add("}");
		save(struct.getFullName(), ls);
	}
	
	private void genConfig() {
		final List<Config> exportConfigs = Config.getExportConfigs();
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = "cfg";
		final String name = Main.cfgmgrName;
		
		ls.add("package " + namespace + ";");
		ls.add(String.format("public class %s {", name));
		ls.add("	public static class DataDir { public static String dir; public static String encoding; }");
		exportConfigs.forEach(c -> {
			final String vtype = c.getType();
			final String cname = c.getName();
			if(!c.isSingle()) {
				ls.add(String.format("	public static java.util.LinkedHashMap<%s, %s> %s;",
					toBoxType(c.getIndexType()), toBoxType(vtype), cname));
			} else {
				ls.add(String.format("	public static %s %s;", toJavaType(vtype), cname));
			}
			}
		);
		ls.add("	public static void load() {");
		exportConfigs.forEach(c -> {
			final String vtype = c.getType();
			final String cname = c.getName();
			ls.add("		{");
				ls.add(String.format("			final cfg.DataStream fs = cfg.DataStream.create(DataDir.dir + \"/%s\", DataDir.encoding);", c.getOutputDataFile()));
				if(!c.isSingle()) {
                    ls.add(String.format("          final java.util.LinkedHashMap<%s, %s> _datas = new java.util.LinkedHashMap<>();",
                            toBoxType(c.getIndexType()), toBoxType(vtype), cname));
					ls.add("			for(int n = fs.getInt() ; n-- > 0 ; ) {");
					ls.add(String.format("				final %s v = %s;", toBoxType(vtype), readType(vtype)));
					ls.add(String.format("				_datas.put(v.%s, v);", c.getIndex()));
					ls.add("			}");
                    ls.add(String.format("          %s = _datas;", cname));
			} else {
				ls.add("			if(fs.getInt() != 1) throw new RuntimeException(\"single conifg size != 1\");");
				ls.add(String.format("			%s = %s;", cname, readType(vtype)));
			}
			ls.add("		}");
		});
		ls.add("	}");
		
		ls.add("}");
		save(namespace + "." + name, ls);
	}

}
