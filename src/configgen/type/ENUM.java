package configgen.type;

import configgen.Utils;
import configgen.data.Type;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public final class ENUM {
	private final static HashMap<String, ENUM> enums = new HashMap<>();
	public static boolean isEnum(String name) {
		return enums.containsKey(name.toLowerCase());
	}
	
	public static ENUM get(String name) {
		return enums.get(name.toLowerCase());
	}
	
	public static void put(String name, ENUM e) {
		enums.put(name.toLowerCase(), e);
	}
	
	public static Collection<ENUM> getExports() {
		return enums.values();
	}
	
	public String getName() {
		return name;
	}

	public String getFullname() {
		return fullname;
	}
	
	public HashMap<String, Integer> getCases() {
		return cases;
	}
	
	public final String getConstValue(String name) {
		Integer value = cases.get(name);
		return value != null ? value.toString() : null;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	private final String namespace;
	private final String name;
	private final HashMap<String, Integer> cases = new LinkedHashMap<>();
	private final HashMap<String, String> aliass = new HashMap<>();
	
	private final String fullname;
	public ENUM(String namespace, Element ele) {
		this.namespace = namespace;
		name = ele.getAttribute("name");
		if(name.isEmpty())
			error("enum名字为空");
		this.fullname = namespace + "." + name;
		//System.out.printf("== xml:%s enum:%s\n", Main.curXml, fullname);
		if(Utils.existType(fullname))
			error("重复类型名!");
		put(fullname, this);
		
		final String NULL = Type.NULL_STR.toUpperCase() ;
		cases.put(NULL, Type.NULL_VALUE);
		aliass.put(Type.NULL_STR, NULL);
		aliass.put(NULL, NULL);
		int enumValue = 0;
		for(Element c : Utils.getChildsByTagName(ele, "const")) {
			final String cname = c.getAttribute("name");
			final String strValue = c.getAttribute("value");
			if(!strValue.isEmpty()) {
				//error(String.format("const:%s value missing", cname));
				enumValue = Integer.parseInt(c.getAttribute("value"));
			}
			if(cases.put(cname, enumValue) != null)
				error(String.format("const:%s 重名!", cname));
			enumValue++;
			aliass.put(cname, cname);
			for(String aliasName : Utils.split(c, "alias")) {
				if(aliass.put(aliasName, cname) != null)
					error(String.format("enum const alias<%s, %s> 重复!", cname, aliasName));
			}
		}
	}
	
	public int getEnumValueByName(String name) {
		final String cname = aliass.get(name);
		if(cname == null)
			error(name + "不是enum:" + this.name + "的合法枚举值");
		return cases.get(cname);
	}
	
	public String getDefaultConstName() {
		return cases.isEmpty() ? "" : cases.keySet().iterator().next();
	}
	
	public void error(String err) {
		Utils.error("enum:%s %s", name, err);
	}
}
