package configgen.data;

import configgen.FlatStream;
import configgen.Utils;
import configgen.type.Field;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedHashMap;
import java.util.Map;

public class FMap extends Type {
	public final Map<Type, Type> values = new LinkedHashMap<Type, Type>();

    private final Field keyDefine;
    private final Field valueDefine;

	public FMap(FStruct host, Field define, FlatStream is) {
		super(host, define);
		this.keyDefine = define.getMapKeyFieldDefine();
		this.valueDefine = define.getMapValueFieldDefine();
		while(!is.isSectionEnd()) {
			final Type key = Type.create(host, keyDefine, is);
			if(values.put(key, Type.create(host, valueDefine, is)) != null) {
				throw new RuntimeException(String.format("field:%s key:%s dunplicate", define, key));
			}
		}
	}

	public FMap(FStruct host, Field define, LuaTable t) {
		super(host, define);
		this.keyDefine = define.getMapKeyFieldDefine();
		this.valueDefine = define.getMapValueFieldDefine();
		for(LuaValue k : t.keys()) {
			final Type key = Type.create(host, keyDefine, k);
			if(values.put(key, Type.create(host, valueDefine, t.get(k))) != null) {
				throw new RuntimeException(String.format("field:%s key:%s dunplicate", define, key));
			}
		}
	}
	
	public FMap(FStruct host, Field define, Element ele) {
		super(host, define);
		this.keyDefine = define.getMapKeyFieldDefine();
		this.valueDefine = define.getMapValueFieldDefine();
		final NodeList nodes = ele.getChildNodes();
		for(int i = 0, n = nodes.getLength() ; i < n ; i++) {
			final Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				final Type key = Type.create(host, keyDefine,  (Element)((Element)node).getElementsByTagName("key").item(0));
				final Type value = Type.create(host, valueDefine, (Element)((Element)node).getElementsByTagName("value").item(0));
				if(values.put(key, value) != null)
					Utils.error("field:%s key:%s dunplicate", define, key);
			}
		}
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Map<").append(define.getFullType()).append(">{");
		for(Map.Entry<Type, Type> e : values.entrySet()) {
			sb.append("<").append(e.getKey()).append(",").append(e.getValue()).append(">,");
		}
		sb.append("}");
		return sb.toString();
	}
	

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public void verifyData() {
		for(Map.Entry<Type, Type> e : values.entrySet()) {
		    e.getKey().verifyData();
            e.getValue().verifyData();
        }
	}

	@Override
	public boolean isNull() {
		return false;
	}
	
}
