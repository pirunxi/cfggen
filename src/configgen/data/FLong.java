package configgen.data;

import org.w3c.dom.Element;

import configgen.FlatStream;
import configgen.type.Field;

public final class FLong extends Type {
	public FLong(FStruct host, Field define, String is) {
		super(host, define);
		value = is.equalsIgnoreCase(NULL_STR) ? NULL_VALUE : Long.parseLong(is);
	}

	public FLong(FStruct host, Field define, long v) {
		super(host, define);
		value = v;
	}
	
	public FLong(FStruct host, Field define, FlatStream is) {
		this(host, define, is.getString());
	}
	
	public FLong(FStruct host, Field define, Element node) {
		this(host, define, node.getFirstChild().getTextContent());
	}

	public final long value;

	public String toString() {
		return "long:" + value;
	}
	
	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FLong)) return false;
		return value == ((FLong)o).value;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}
	
	@Override
	public boolean isNull() {
		return value == NULL_VALUE;
	}
}
