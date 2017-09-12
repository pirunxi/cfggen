package configgen.data;

import org.w3c.dom.Element;

import configgen.FlatStream;
import configgen.type.ENUM;
import configgen.type.Field;

public final class FEnum extends Type {
	public FEnum(FStruct host, Field define, String is) {
		super(host, define);
		enumName = is;
		value = ENUM.get(define.getType()).getEnumValueByName(is);
	}
	
	public FEnum(FStruct host, Field define, FlatStream is) {
		this(host, define, is.getString());
	}
	
	public FEnum(FStruct host, Field define, Element node) {
		this(host, define, node.getFirstChild().getTextContent());
	}

	public final String enumName;
	public final int value;
	
	public String toString() {
		return "enum:" + value;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FEnum)) return false;
		return value == ((FEnum)o).value;
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
