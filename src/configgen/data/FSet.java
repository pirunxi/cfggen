package configgen.data;

import configgen.FlatStream;
import configgen.Utils;
import configgen.type.Field;
import org.luaj.vm2.LuaTable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;

public class FSet extends Type {
	final public Set<Type> values = new HashSet<Type>();
	public FSet(FStruct host, Field define, LuaTable t) {
		super(host, define);
		Field valueDefine = define.getValueFieldDefine();

		for(int i = 1, n = t.rawlen() ; i <= n ; i++) {
			Type value = Type.create(host, valueDefine, t.rawget(i));
			if(!values.add(value)) {
				throw new RuntimeException(String.format("field:%s value:%s duplicate!", define, value));
			}
		}
	}

	public FSet(FStruct host, Field define, FlatStream is) {
		super(host, define);
		Field valueDefine = define.getValueFieldDefine();
		while(!is.isSectionEnd()) {
			Type value = Type.create(host, valueDefine, is);
			if(!values.add(value)) {
				throw new RuntimeException(String.format("field:%s value:%s duplicate!", define, value));
			}
		}
	}
	
	public FSet(FStruct host, Field define, Element ele) {
		super(host, define);
		Field valueDefine = define.getValueFieldDefine();
		final NodeList nodes = ele.getChildNodes();
		for(int i = 0, n = nodes.getLength() ; i < n ; i++) {
			final Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Type value = Type.create(host, valueDefine, (Element)node);
				if(!values.add(value))
					Utils.error("field:%s value:%s duplicate!", define, value);
			}
		}
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Set<").append(define.getFullType()).append(">{");
		values.forEach(v -> sb.append(v).append(","));
		sb.append("}");
		return sb.toString();
	}
	

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public void verifyData() {
        for (Type d : values) {
            d.verifyData();
        }
	}

	@Override
	public boolean isNull() {
		return false;
	}
}
