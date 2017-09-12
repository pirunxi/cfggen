package configgen.type;

import configgen.Utils;
import org.w3c.dom.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Field {
	private final Struct parent;
	private final String name;
	private final String fullType;
	private final List<String> types;
	private final List<String> localizeds;

    private String compounddelimiter;

	private final HashSet<String> indexs = new HashSet<>();
	private final HashSet<String> groups = new HashSet<>();
	private final List<String> refs = new ArrayList<>();
    private final List<String> refPath = new ArrayList<>();
	
	private final static HashSet<String> RawTypes = new HashSet<>(Arrays.asList("bool", "int", "float", "long", "string"));
	private final static HashSet<String> ConTypes = new HashSet<>(Arrays.asList("list", "set", "map"));
	private final static HashSet<String> ReserveNames = new HashSet<>(Arrays.asList("end", "base", "super"));//, "typeid", "friend"));

	private final static Pattern namePattern = Pattern.compile("[a-zA-Z]\\w*");
	public Field(Struct parent, String name, String compounddelimiter, String fulltype, String[] types, String[] indexs, String[] refs, String[] refPath, String[] groups, String[] localizeds) {
		this.parent = parent;
		this.name = name;
		this.fullType = fulltype;
		this.types = Arrays.asList(types);
		if(this.types.isEmpty())
			error("没有定义 type");
		this.localizeds = Arrays.asList(localizeds);
		
		for(int i = 0 ; i < types.length ; i++) {
			String t = types[i];
			if(!isRaw(t) && !isContainer(t) && !t.contains("."))
				types[i] = parent.getNamespace() + "." + types[i];
		}

		if(name.isEmpty())
			error("没有定义 name");
		final Matcher matcher = namePattern.matcher(name);
		if(!matcher.matches())
			error("非法变量名:" + name);
		if(ReserveNames.contains(name))
			error("保留关键字:" + name);

        this.compounddelimiter = compounddelimiter;
		Collections.addAll(this.indexs, indexs);
		Collections.addAll(this.refs, refs);
		Collections.addAll(this.refPath, refPath);
		Collections.addAll(this.groups, groups);

		if(this.groups.isEmpty()) 
			this.groups.add("all");
	}
	
	public Field(Struct parent, Element data) {
		this(
			parent, 
			data.getAttribute("name"),
            data.getAttribute("delimiter"),
			data.getAttribute("type"),
			Utils.split(data, "type"),
			Utils.split(data, "index"),
			Utils.split(data, "ref"),
            Utils.split(data, "refpath", ";|,|:"),
			Utils.split(data, "group"),
			Utils.split(data, "localized")
			);	
	}
	
	private Field(Struct parent, String name, String delimiter, String fullType, List<String> types, HashSet<String> groups, List<String> refs, List<String> refPaths, List<String> localizeds) {
		this.parent = parent;
		this.name = name;
		this.fullType = fullType;
		this.types = types;
		this.localizeds = localizeds;
		this.groups.addAll(groups);
        this.compounddelimiter = delimiter;
        this.refs.addAll(refs);
        this.refPath.addAll(refPaths);
	}


	public Field getValueFieldDefine() {
		Struct s = Struct.get(types.get(1));
		String delimiter = s != null ? s.getdelimiter() : "";
		return new Field(parent, name, delimiter, fullType, types.subList(1, types.size()), groups,
				(refs.isEmpty() ? Collections.emptyList() : Collections.singletonList(refs.get(0))), refPath, localizeds);
    }

    public Field getMapKeyFieldDefine() {
		Struct s = Struct.get(types.get(1));
		String delimiter =  s != null ? s.getdelimiter() : "";
		return new Field(parent, name, delimiter, fullType, types.subList(1, types.size()), groups,
				(refs.isEmpty() ? Collections.emptyList() : Collections.singletonList(refs.get(0))),
				(refPath.isEmpty() ? Collections.emptyList() : Collections.singletonList(refPath.get(0))),
				localizeds.isEmpty() ? Collections.emptyList() : Collections.singletonList(localizeds.get(0)));
    }

    public Field getMapValueFieldDefine() {
		Struct s = Struct.get(types.get(2));
		String delimiter =  s != null ? s.getdelimiter() : "";
		return  new Field(parent, name, delimiter, fullType, types.subList(2, types.size()), groups,
				(refs.size() <= 1 ? Collections.emptyList() : Collections.singletonList(refs.get(1))),
				(refPath.size() <= 1 ? Collections.emptyList() : Collections.singletonList(refPath.get(1))),
				(localizeds.size() <= 1 ? Collections.emptyList() : Collections.singletonList(localizeds.get(1))));
    }

	public boolean isLocalized() {
		return localizeds.size() > 0 && !localizeds.get(0).isEmpty();
	}

	public String getName() {
		return name;
	}
	
	public final String getFullType() {
		return fullType;
	}

	public String getType() {
		return types.get(0);
	}
	
	public List<String> getTypes() {
		return types;
	}

    public boolean isCompound() {
        return !compounddelimiter.isEmpty();
    }

    public String getdelimiter() {
        return compounddelimiter;
    }
	
	public final HashSet<String> getGroups() {
		return groups;
	}
	
	public final String getRef() {
		return refs.size() >= 1 ? refs.get(0) : "";
	}

    public final List<String> getRefPath() {
        return refPath;
    }

	public final boolean checkInGroup(Set<String> gs) {
		if(groups.contains("all")) return true;
		if(gs.contains("all")) return true;
		for(String g : gs) {
			if(groups.contains(g))
				return true;
		}
		return false;
	}

	public final HashSet<String> getIndexs() {
		return indexs;
	}

	public boolean isRaw() {
		return isRaw(types.get(0));
	}

	public static boolean isRaw(String type) {
		return RawTypes.contains(type);
	}
	
	public boolean isContainer() {
		return isContainer(types.get(0));
	}

	public static boolean isContainer(String type) {
		return ConTypes.contains(type);
	}
	
	public boolean isStruct() {
		return isStruct(types.get(0));
	}
	
	public boolean isDynamic() {
		Struct s = Struct.get(types.get(0));
		return s != null && s.isDynamic();
	}
	
	public static boolean isStruct(String type) {
		return Struct.isStruct(type);
	}
	
	public boolean isEnum() {
		return ENUM.isEnum(types.get(0));
	}
	
	public static boolean isEnum(String type) {
		return ENUM.isEnum(type);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Field{name=").append(name).append(",types={");
		for(String t : types) {
			sb.append(t).append(",");
		}
		sb.append("}, groups={");
		for(String g : groups) {
			sb.append(g).append(",");
		}
		sb.append("}}");
		return sb.toString();
	}
	
	public void checkType(int idx) {
		if(types.size() <= idx)
			error("没有定义 type");
	}
	
	public void error(String err) {
		throw new RuntimeException(String.format("%s.%s %s", parent, name, err));
	}

	public boolean isRawOrEnumOrStruct() {
		return (isRaw()
				|| isEnum()
				|| isStruct());
	}
	public static boolean isRawOrEnumOrStruct(String typeName) {
		return (isRaw(typeName)
			|| isEnum(typeName)
			|| isStruct(typeName));
	}
	
	public void verifyDefine() {
		checkType(0);
		final String type = getType();
		if(isRaw()) {
			
		} else if(isStruct()) {
            if(compounddelimiter.isEmpty())
                compounddelimiter = Struct.get(types.get(0)).getdelimiter();
		} else if(isEnum()) {
			
		} else if(isContainer()) {
			if("map".equals(type)) {
				checkType(1);
				final String keyType = types.get(1);
				if(!isRawOrEnumOrStruct(keyType))
					error("非法的map key类型:" + keyType);
				checkType(2);
				final String valueType = types.get(2);
				if(!isRawOrEnumOrStruct(valueType))
					error("非法的map value类型:" + valueType);
			} else if("set".equals(type)) {
				checkType(1);
				final String valueType = types.get(1);
				if(!isRawOrEnumOrStruct(valueType))
					error("非法的set value类型:" + valueType);
			} else if("list".equals(type)) {
				checkType(1);
				final String valueType = types.get(1);
				if(!isRawOrEnumOrStruct(valueType))
					error("非法的list value类型:" + valueType);
				if(!indexs.isEmpty()) {
					if(!isStruct(valueType)) {
						error("list的 value 类型:" + valueType + "必须是struct才能index");
					}
				}
			}
			for(int i = 0 ; i < types.size() ; i++) {
				if(types.get(i).equals("text"))
					types.set(i, "string");
			}
		} else {
			error("未知类型:" + type);
		}
		for(String name : groups) {
			if(!Group.isGroup(name))
				error("未知 group:" + name);
		}

		if(types.get(0).equals("list")) {
			final String valueType = types.get(1);
			if(Field.isStruct(valueType)) {
				Struct s = Struct.get(valueType);
				for(String idx : indexs) {
					if(s.getField(idx) == null)
						error("index:" + idx + " 不是struct:" + valueType + " 的字段!");
				}
			}
		}
	}

}
