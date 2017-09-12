package configgen.lans.scala;

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
		final String outFile = String.format("%s/%s.scala", Main.codeDir, fullTypeName.replace('.', '/'));
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
					return String.format("fs.getObject(fs.getString()).asInstanceOf[%s]", type);
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
		return toBoxType(type);
	}

	private String toValidName(String fieldName) {
		switch (fieldName) {
			case "type" :
			case "wait" : return "_" + fieldName;
			default : return fieldName;
		}
	}

	private String toBoxType(String type) {
		switch(type) {
			case "bool": return "Boolean";
			case "int": return "Int";
			case "long": return "Long";
			case "float": return "Float";
			case "string": return "String";
			default : return ENUM.isEnum(type) ? "Int" : type;
		}
	}

	private void genEnum(ENUM e) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = e.getNamespace();
		ls.add("package " + namespace);
		final String name = e.getName();
		ls.add(String.format("object %s {", name));
		ls.addAll(e.getCases().entrySet().stream().map(me ->
				String.format("	val %s = %d", me.getKey(), me.getValue())).collect(Collectors.toList()));
		ls.add(String.format("	val enums = Vector(%s)",
				e.getCases().entrySet().stream().filter(ee -> !ee.getKey().equalsIgnoreCase("null"))
				.map(ee-> ee.getValue().toString()).collect(Collectors.joining(" ,"))));
		ls.add("}");
		save(e.getFullname(), ls);
	}

	private void genStruct(Struct struct) {
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = struct.getNamespace();
		ls.add("package " + namespace);
		
		final String base = struct.getBase();
		final String name = struct.getName();
		final boolean isDynamic = struct.isDynamic() ;
		ls.add(String.format("%s class %s(fs : perfect.cfg.DataStream) extends %s {", (isDynamic ? "abstract" : "sealed"), name, (base.isEmpty() ? "perfect.cfg.CfgObject" : base + "(fs)")));
		
		if(!isDynamic) {
			ls.add(String.format("	override val getTypeId = %s.TYPEID", name));
		}
		
//		final ArrayList<String> ds = new ArrayList<>();
//		final ArrayList<String> cs = new ArrayList<>();
		
		for(Field f : struct.getFields()) {
			String ftype = f.getType();
			String jtype = toJavaType(ftype);
			String fname = toValidName(f.getName());
			final List<String> ftypes = f.getTypes();
			if(f.checkInGroup(Main.groups)) {
				if(f.isRawOrEnumOrStruct()) {
					ls.add(String.format("	val %s : %s = %s", fname, jtype, readType(ftype)));
				} else if(f.isContainer()) {
					switch(ftype) {
						case "list": {
							final String vtype = ftypes.get(1);
							final String bvtype = toBoxType(ftypes.get(1));
							ls.add(String.format("	val %s : scala.collection.mutable.ListBuffer[%s] = scala.collection.mutable.ListBuffer()", fname, bvtype));

							if(!f.getIndexs().isEmpty()) {
								final Struct s = Struct.get(vtype);
								for(String idx : f.getIndexs()) {
									final String bktype = toBoxType(s.getField(idx).getType());
									final String idxFieldName = fname + "_" + idx;
									ls.add(String.format("	val %s : scala.collection.mutable.Map[%s,%s] =scala.collection.mutable.Map()",
											idxFieldName, bktype, bvtype));
								}
							}

							ls.add("		for(_ <- 1 to fs.getInt()) {");
							if(f.getIndexs().isEmpty()) {
								ls.add(String.format("			this.%s += %s", fname, readType(vtype)));
							} else {
								ls.add(String.format("			val _v = %s", readType(vtype)));
								ls.add(String.format("			this.%s += _v", fname));
								final Struct s = Struct.get(vtype);
								for(String idx : f.getIndexs()) {
									final String idxFieldName = fname + "_" + idx;
									ls.add(String.format("			this.%s += (_v.%s -> _v)", idxFieldName, idx));
								}
							}
							ls.add("		}");
							break;
						}
						case "set": {
							final String vtype = ftypes.get(1);
							ls.add(String.format("	val %s : scala.collection.mutable.Set[%s] = scala.collection.mutable.Set()", fname, toBoxType(vtype)));
							ls.add("		for(_ <- 1 to fs.getInt()) {");
							ls.add(String.format("			this.%s.add(%s)", fname, readType(vtype)));
							ls.add("		}");
							break;
						}
						case "map": {
							final String ktype = ftypes.get(1);
							final String vtype = ftypes.get(2);
							ls.add(String.format("	val %s : scala.collection.mutable.Map[%s,%s] = scala.collection.mutable.Map()",
									fname, toBoxType(ktype), toBoxType(vtype)));
							ls.add("		for(_ <- 1 to fs.getInt()) {");
							ls.add(String.format("			this.%s.put(%s, %s)", fname, readType(ktype), readType(vtype)));
							ls.add("		}");
							break;
						}
					}
				} else {
					Utils.error("unknown type:" + ftype);
				}
			}
		}

		ls.add("}");

		ls.add(String.format("object %s {", name));

		if(!isDynamic) {
			ls.add(String.format("	val TYPEID : Int = %s", struct.getTypeId()));
		}
		for(Const c : struct.getConsts()) {
			final String type = c.getType();
			final String value = c.getValue();
			final String cname = c.getName();
			if(Field.isRaw(type)) {
				ls.add(String.format("	val %s : %s = %s",
						cname, toJavaType(type), toJavaValue(type, value)));
			} else {
				switch(type) {
					case "list:int" :
						ls.add(String.format("	val %s : Vector[Int] = Vector(%s)", cname, value));
						break;
					case "list:float" :
						ls.add(String.format("	val %s : Vector[Double] = Vector(%s)", cname, value));
						break;
					default:
						Utils.error("struct:%s const:%s unknown type:%s", struct.getFullName(), c.getName(), type);
				}
			}
		}
		ls.add("}");

		save(struct.getFullName(), ls);
	}
	
	private void genConfig() {
		final List<Config> exportConfigs = Config.getExportConfigs();
		final ArrayList<String> ls = new ArrayList<>();
		final String namespace = "cfg";
		final String name = Main.cfgmgrName;
		
		ls.add("package " + namespace + "");
		ls.add(String.format("object %s {", name));
		ls.add("	object DataDir { var dir : String = _; var encoding : String = _; }");
		exportConfigs.forEach(c -> {
			final String vtype = c.getType();
			final String cname = c.getName();
			if(!c.isSingle()) {
				ls.add(String.format("	var %s : java.util.LinkedHashMap[%s, %s] = _",
					cname, toBoxType(c.getIndexType()), toBoxType(vtype)));
			} else {
				ls.add(String.format("	var %s : %s = _", cname, toJavaType(vtype)));
			}
			}
		);
		ls.add("	def load() {");
		exportConfigs.forEach(c -> {
			final String vtype = c.getType();
			final String cname = c.getName();
			ls.add("		{");
				ls.add(String.format("			val fs = perfect.cfg.DataStream.create(DataDir.dir + \"/%s\", DataDir.encoding)", c.getOutputDataFile()));
				if(!c.isSingle()) {
                    ls.add(String.format("          val _datas = new java.util.LinkedHashMap[%s, %s]()",
                            toBoxType(c.getIndexType()), toBoxType(vtype)));
					ls.add("			for(_ <- 1 to fs.getInt()) {");
					ls.add(String.format("				val v = %s", readType(vtype)));
					ls.add(String.format("				_datas.put(v.%s, v)", toValidName(c.getIndex())));
					ls.add("			}");
                    ls.add(String.format("          %s = _datas", cname));
				} else {
					ls.add("			if(fs.getInt() != 1) throw new RuntimeException(\"single conifg size != 1\");");
					ls.add(String.format("			%s = %s", cname, readType(vtype)));
				}
			ls.add("		}");
		});
		ls.add("	}");
		
		ls.add("}");
		save(namespace + "." + name, ls);
	}

}
