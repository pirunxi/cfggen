package configgen.data;

import configgen.FlatStream;
import configgen.type.Field;
import org.w3c.dom.Element;

public final class FInt extends Type {
	public FInt(FStruct host, Field define, String is) {
		super(host, define);
		value = is.equalsIgnoreCase(NULL_STR) ? NULL_VALUE : Integer.parseInt(is);
	}

	public FInt(FStruct host, Field define, int v) {
		super(host, define);
		value = v;
	}
	
	public FInt(FStruct host, Field define, FlatStream is) {
		this(host, define, is.getString());
	}
	
	public FInt(FStruct host, Field define, Element node) {
		this(host, define, node.getFirstChild().getTextContent());
	}

	public final int value;

	public String toString() {
		return "int:" + value;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FInt)) return false;
		return value == ((FInt)o).value;
	}
	
	@Override
	public int hashCode() {
		return value;
	}
	
	@Override
	public boolean isNull() {
		return value == NULL_VALUE;
	}
}
