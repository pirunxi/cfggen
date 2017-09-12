package configgen.lans.cs;

import configgen.Generator;
import configgen.Main;
import configgen.Utils;
import configgen.type.*;

import java.util.ArrayList;
import java.util.Collection;
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
		final String outFile = String.format("%s/%s.cs", Main.codeDir, fullTypeName.replace('.', '/'));
		Utils.save(outFile, code);
	}

	private String readType(String type) {
		switch(type) {
			case "bool": return "fs.GetBool()";
			case "int":	 return "fs.GetInt()";
			case "long": return "fs.GetLong()";
			case "float":  return "fs.GetFloat()";
			case "string": return "fs.GetString()";
			default: {
				if(ENUM.isEnum(type)) return "fs.GetInt()";
				return Struct.isDynamic(type) ?
					String.format("(%s)fs.GetObject(fs.GetString())", type) :
					String.format("new %s(fs)", type);
			}
		}
	}

	private String toCsType(String type) {
		return ENUM.isEnum(type) ? "int" : type;
	}

	private String toCsValue(String type, String value) {
		switch(type) {
			case "string": return "\"" + value + "\"";
			case "float": return value + "f";
			default: return value;
		}
	}

	private void genEnum(ENUM e) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = e.getNamespace();
		ls.add("namespace " + namespace + "{");
		final String name = e.getName();
		ls.add(String.format("public sealed class %s {", name));
		ls.addAll(e.getCases().entrySet().stream().map(me ->
				String.format("public const int %s = %d;", me.getKey(), me.getValue())).collect(Collectors.toList()));
		ls.add("}");
		ls.add("}");
		final String code = ls.stream().collect(Collectors.joining("\n"));
		final String outFile = String.format("%s/%s/%s.cs", Main.codeDir, namespace.replace('.', '/'), name);
		Utils.save(outFile, code);
	}

	private void genStructConsts(Struct struct, ArrayList<String> ls) {
		for(Const c : struct.getConsts()) {
			final String type = c.getType();
			final String value = c.getValue();
			final String cname = c.getName();
			if(Field.isRaw(type)) {
				ls.add(String.format("	public const %s %s = %s;",
						toCsType(type), cname, toCsValue(type, value)));
			} else {
				switch(type) {
					case "list:int" :
						ls.add(String.format("	public static readonly int[] %s = {%s};", cname, value));
						break;
					case "list:float" :
						ls.add(String.format("	public static readonly double[] %s = {%s};", cname, value));
						break;
					default:
						Utils.error("unknow const type:" + type);
				}
			}
		}
	}
	
	private void genStruct(Struct struct) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = struct.getNamespace();
		ls.add("using System;");
		ls.add("namespace " + namespace + "{");
		
		final String base = struct.getBase();
		final String name = struct.getName();
		final boolean isDynamic = struct.isDynamic() ;
		ls.add(String.format("public %s class %s : %s {", isDynamic ? "abstract" : "sealed", name, (base.isEmpty() ? "cfg.CfgObject" : base)));
		
		if(!isDynamic) {
			ls.add(String.format("public const int TYPEID = %s;", struct.getTypeId()));
			ls.add("public override int GetTypeId() { return TYPEID; }");
		}

		genStructConsts(struct, ls);

		final ArrayList<String> ds = new ArrayList<>();
		final ArrayList<String> cs = new ArrayList<>();

		cs.add(String.format("public %s(cfg.DataStream fs) %s {", name, base.isEmpty() ? "" : ": base(fs)"));
		
		for(Field f : struct.getFields()) {
			final String ftype = f.getType();
			final String jtype = toCsType(ftype);
			final String fname = f.getName();
			final List<String> ftypes = f.getTypes();
			if(f.checkInGroup(Main.groups)) {
				if(f.isRawOrEnumOrStruct()) {
					ds.add(String.format("public readonly %s %s;", jtype, fname));
					cs.add(String.format("this.%s = %s;", fname, readType(ftype)));
				} else if(f.isContainer()) {
					switch(ftype) {
						case "list": {
							final String vtype = ftypes.get(1);
							final String jvtype = toCsType(vtype);
							ds.add(String.format("public readonly System.Collections.Generic.List<%s> %s = new System.Collections.Generic.List<%s>();", jvtype, fname, jvtype));
							cs.add("for(int n = fs.GetInt(); n-- > 0 ; ) {");
							if(f.getIndexs().isEmpty()) {
								cs.add(String.format("this.%s.Add(%s);", fname, readType(vtype)));
							} else {
								cs.add(String.format("var _v = %s;", readType(vtype)));
								final Struct s = Struct.get(vtype);
								for(String idx : f.getIndexs()) {
									final String jktype = toCsType(s.getField(idx).getType());
									final String idxFieldName = fname + "_" + idx;
									ds.add(String.format("public readonly System.Collections.Generic.Dictionary<%s, %s> %s = new System.Collections.Generic.Dictionary<%s, %s>();",
											jktype, jvtype, idxFieldName, jktype, jvtype));
									cs.add(String.format("this.%s.Add(_v.%s, _v);", idxFieldName, idx));
								}
							}
							cs.add("}");
							break;
						}
						case "set": {
							final String vtype = ftypes.get(1);
							final String jvtype = toCsType(vtype);
							ds.add(String.format("public readonly System.Collections.Generic.HashSet<%s> %s = new System.Collections.Generic.HashSet<%s>();", jvtype, fname, jvtype));
							cs.add("for(int n = fs.GetInt(); n-- > 0 ; ) {");
							cs.add(String.format("this.%s.Add(%s);", fname, readType(vtype)));
							cs.add("}");
							break;
						}
						case "map": {
							final String ktype = ftypes.get(1);
							final String vtype = ftypes.get(2);
							final String jktype = toCsType(ktype);
							final String jvtype = toCsType(vtype);
							ds.add(String.format("public readonly System.Collections.Generic.Dictionary<%s, %s> %s = new System.Collections.Generic.Dictionary<%s, %s>();", jktype, jvtype, fname, jktype, jvtype));
							cs.add("for(int n = fs.GetInt(); n-- > 0 ; ) {");
							cs.add(String.format("var _k = %s;", readType(ktype)));
							cs.add(String.format("this.%s[_k] = %s;", fname, readType(vtype)));
							cs.add("}");
							break;
						}
					}
				} else {
					Utils.error("unknown type:" + ftype);
				}
			}
		}
		
		cs.add("}");
		ls.addAll(ds);
		ls.addAll(cs);
		
		ls.add("}");
		ls.add("}");
		save(struct.getFullName(), ls);
	}
	
	private void genConfig() {
		final List<Config> exportConfigs = Config.getExportConfigs();
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = "cfg";
		final String name = Main.cfgmgrName;
		ls.add("using System;");
		ls.add("namespace " + namespace + "{");
		ls.add(String.format("public class %s {", name));
		ls.add("public class DataDir {");
		ls.add("public static string Dir { set; get;} ");
		ls.add("public static string Encoding { set; get; }");
		ls.add("}");
		exportConfigs.forEach(c -> {
			final String cname = c.getName();
			final String ctype = c.getType();
			if(!c.isSingle()) {
				final String itype = toCsType(c.getIndexType());
				ls.add(String.format("public static readonly System.Collections.Generic.Dictionary<%s, %s> %s = new System.Collections.Generic.Dictionary<%s, %s>();",
					itype, ctype, cname, itype, ctype));
			} else {
				ls.add(String.format("public static %s %s;", ctype, cname));
			}
		});

		ls.add("public static void Load() {");
		exportConfigs.forEach(c -> {
			ls.add("{");ls.add(String.format("var fs = cfg.DataStream.Create(DataDir.Dir + \"/%s\", DataDir.Encoding);", c.getOutputDataFile()));
			final String ctype = c.getType();
			final String cname = c.getName();
			if(!c.isSingle()) {
				ls.add(String.format("%s.Clear();", cname));
				ls.add("for(var n = fs.GetInt() ; n-- > 0 ; ) {");
				ls.add(String.format("var _v = %s;", readType(ctype)));
				ls.add(String.format("%s.Add(_v.%s, _v);", cname, c.getIndex()));
				ls.add("}");
			} else {
				ls.add("if(fs.GetInt() != 1) throw new Exception(\"single config size!=1\");");
				ls.add(String.format("%s = %s;", cname, readType(ctype)));
			}
			ls.add("}");
		});
		ls.add("}");
		
		ls.add("}");
		ls.add("}");
	
		save(namespace + "." + name, ls);
	}

	public void genMarshallCode() {
		genEnumXmlMarshal(ENUM.getExports());
		Struct.getExports().forEach(s -> genStructXmlMarshallCode(s));
	}
	
	
	public String toMarshalType(String type) {
		int idx = type.lastIndexOf('.');
		return idx < 0 ? type : xmlPrefix + type;
	}
	
	private final static String xmlPrefix = "xml.";
	private final static String enumClass = "Enums";

	public String toXmlJavaType(String rawType) {
		return ENUM.isEnum(rawType) ? "string" : rawType;
	}
	void genEnumXmlMarshal(Collection<ENUM> enums) {
		final ArrayList<String> ls = new ArrayList<String>();
		final String namespace = xmlPrefix + "cfg";
		ls.add("using System.Collections.Generic;");
		ls.add("namespace " + namespace + "{");
		final String name = enumClass;
		ls.add(String.format("public sealed class %s {", name));
		for(ENUM e : enums) {
			ls.add(String.format("public static List<string> %s = new List<string>{%s};",
				e.getName(), e.getCases().keySet().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","))));
		}
		ls.add("}");
		ls.add("}");
		final String code = ls.stream().collect(Collectors.joining("\n"));
		final String outFile = String.format("%s/%s.%s.cs", Main.csmarshalcodeDir, namespace, name);
		Utils.save(outFile, code);
	}
	
	String getNamespaceOfType(String type) {
		int idx = type.lastIndexOf('.');
		return idx < 0 ? type : type.substring(0, idx);
	}
	
	String upperFirstChar(String type) {
		return type.substring(0, 1).toUpperCase() + type.substring(1);
	}
	
	String readXmlType(String os, String type) {
		final String marshalType = toMarshalType(type);
		switch(type) {
			case "bool":
			case "int":
			case "long":
			case "float":
			case "string":
			 return String.format("Read%s(%s)", upperFirstChar(type), os);
			default: {
				Struct struct = Struct.get(type);
				return struct.isDynamic() ?
					String.format("ReadDynamicObject<%s>(%s, \"%s\")", marshalType, os, getNamespaceOfType(marshalType))
				:   String.format("ReadObject<%s>(%s, \"%s\")", marshalType, os, marshalType);
			}
		}
	}

	String getRawTypeDefaultValue(String type) {
		switch (type) {
			case "bool": return "false";
			case "int":
			case "long": return "0";
			case "float": return "0f";
			case "string": return "\"\"";
		}
		throw new RuntimeException("unknow rawtype:" + type);
	}

	private void genStructXmlMarshallCode(Struct struct) {
		final ArrayList<String> ls = new ArrayList<String>();
		final String namespace =  xmlPrefix + struct.getNamespace();
		ls.add("using System;");
		ls.add("namespace " + namespace + " {");
		
		final String base = struct.getBase();
		final String name = struct.getName();
		final boolean isDynamic = struct.isDynamic();
		ls.add(String.format("public %s class %s %s {", isDynamic ? "abstract" : "partial", name, (base.isEmpty() ? ": xml.cfg.XmlMarshaller" : ": " + toMarshalType(base))));
		
		genStructConsts(struct, ls);
		
		final ArrayList<String> ds = new ArrayList<String>();
		final ArrayList<String> ws = new ArrayList<String>();
		final ArrayList<String> rs = new ArrayList<String>();
		
		final String VAR1 = "_1";
		final String VAR2 = "_2";
		final String VAR3 = "_3";
		ws.add(String.format("public override void Write(System.IO.TextWriter %s) {", VAR1));
		rs.add(String.format("public override void Read(System.Xml.XmlNode %s) {", VAR1));
		if(!base.isEmpty()) {
			ws.add(String.format("base.Write(%s);", VAR1));
			rs.add(String.format("base.Read(%s);", VAR1));
		}
		
		rs.add(String.format("foreach (System.Xml.XmlNode %s in GetChilds(%s))", VAR2, VAR1));
		rs.add("{");
		rs.add(String.format("switch(%s.Name)", VAR2));
		rs.add("{");
		
		for(Field f : struct.getFields()) {
			String ftype = f.getType();
			String jtype = toXmlJavaType(ftype);
			final String fname = f.getName();
			final List<String> ftypes = f.getTypes();
			ws.add(String.format("Write(%s, \"%s\", this.%s);", VAR1, fname, fname));
			if (f.isRaw()) {
				ds.add(String.format("public %s %s = %s;", jtype, fname, getRawTypeDefaultValue(ftype)));
				rs.add(String.format("case \"%s\": this.%s = %s; break;", fname, fname, readXmlType(VAR2, jtype)));
			} else if (f.isStruct()) {
				ds.add(String.format("public %s %s;", toMarshalType(jtype), fname));
				rs.add(String.format("case \"%s\": this.%s = %s; break;", fname, fname, readXmlType(VAR2, jtype)));
			} else if(f.isEnum()) {
				final ENUM e = ENUM.get(ftype);
				ds.add(String.format("public string %s = \"%s\";", fname, e.getDefaultConstName()));
				rs.add(String.format("case \"%s\": this.%s = %s; break;", fname, fname, readXmlType(VAR2, "string")));
				ws.add(String.format("if(!%scfg.%s.%s.Contains(this.%s)) throw new Exception(\"%s.%s=\" + this.%s + \" isn't valid enum value\");", 
					xmlPrefix, enumClass, e.getName(), fname, e.getName(), fname, fname));
			} else if (f.isContainer()) {
				switch (ftype) {
				case "list": {
					final String valueType = toXmlJavaType(ftypes.get(1));
					ds.add(String.format(
							"public readonly System.Collections.Generic.List<%s> %s = new System.Collections.Generic.List<%s>();",
							toMarshalType(valueType), fname, toMarshalType(valueType)));
					rs.add(String.format("case \"%s\": GetChilds(%s).ForEach(%s => this.%s.Add(%s)); break;",
						fname, VAR2, VAR3, fname, readXmlType(VAR3, valueType)));
					break;
				}
				case "set": {
					final String valueType = toXmlJavaType(ftypes.get(1));
					ds.add(String.format(
							"public readonly System.Collections.Generic.HashSet<%s> %s = new System.Collections.Generic.HashSet<%s>();",
							toMarshalType(valueType), fname, toMarshalType(valueType)));
					rs.add(String.format("case \"%s\": GetChilds(%s).ForEach(%s => this.%s.Add(%s)); break;",
							fname, VAR2, VAR3, fname, readXmlType(VAR3, valueType)));
					break;
				}
				case "map": {
					final String keyType = toXmlJavaType(ftypes.get(1));
					final String valueType = toXmlJavaType(ftypes.get(2));
					ds.add(String.format(
							"public readonly System.Collections.Generic.Dictionary<%s, %s> %s = new System.Collections.Generic.Dictionary<%s, %s>();",
							toMarshalType(keyType), toMarshalType(valueType), fname, toMarshalType(keyType),
							toMarshalType(valueType)));
					rs.add(String.format("case \"%s\": GetChilds(%s).ForEach(%s => this.%s.Add(%s, %s)); break;",
							fname, VAR2, VAR3, fname,
							readXmlType(String.format("GetOnlyChild(%s, \"key\")", VAR3), keyType), 
							readXmlType(String.format("GetOnlyChild(%s, \"value\")", VAR3), valueType)));
					break;
				}
				}
			} else {
				throw new RuntimeException("unknown type:" + jtype);
			}
		}
		
		ws.add("}");
		rs.add("}");
		rs.add("}");
		rs.add("}");
		ls.addAll(ds);
		ls.addAll(ws);
		ls.addAll(rs);
		
		ls.add("}");
		ls.add("}");
		
		final String code = ls.stream().collect(Collectors.joining("\n"));
		//Main.println(code);
		final String outFile = String.format("%s/%s.%s.cs", Main.csmarshalcodeDir, namespace, name);
		Utils.save(outFile, code);
	}

}
