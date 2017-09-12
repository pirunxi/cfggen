package configgen.data;

import org.w3c.dom.Element;

import configgen.FlatStream;
import configgen.Utils;
import configgen.type.Field;

public class FBool extends Type {
	public FBool(FStruct host, Field define, boolean v) {
		super(host, define);
		value = v;
	}

	public FBool(FStruct host, Field define, String v) {
		super(host, define);
		value = Boolean.parseBoolean(v);
	}
	
	public FBool(FStruct host, Field define, FlatStream is) {
		super(host, define);
		value = is.getBool();
	}
	
	public FBool(FStruct host, Field define, Element node) {
		super(host, define);
		final String s = node.getFirstChild().getTextContent().toLowerCase();
		if(s.equals("true"))
			value = true;
		else if(s.equals("false"))
			value = false;
		else {
			Utils.error("%s is not boolean value", s);
			value = false;
		}
	}

	public final boolean value;
	
	public String toString() {
		return "bool:" + value;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FBool)) return false;
		return value == ((FBool)o).value;
	}
	
	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}

	@Override
	public boolean isNull() {
		return false;
	}
	
}
