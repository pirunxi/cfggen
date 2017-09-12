package configgen.data;

import configgen.FlatStream;
import configgen.Utils;
import configgen.type.Field;
import configgen.type.Struct;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class FStruct extends Type {
	private final String type;
	private ArrayList<Type> values = new ArrayList<Type>();

	public FStruct(FStruct host, Field define, String type, FlatStream is) {
		super(host, define);
		this.type = type;
		load(Struct.get(type), is);
	}

	public FStruct(FStruct host, Field define, String type, LuaTable g) {
		super(host, define);
		this.type = type;
		load(Struct.get(type), g);
	}
	
	public FStruct(FStruct host, Field define, String type, Element ele) {
		super(host, define);
		this.type = type;
		load(Struct.get(type), ele);
	}

	public final ArrayList<Type> getValues() {
		return values;
	}

	public final void setValues(ArrayList<Type> values) {
		this.values = values;
	}

	public final String getType() {
		return type;
	}
	
	public final Type getField(String name) {
		for(Type t : values) {
			if(t.getDefine().getName().equals(name))
				return t;
		}
		return null;
	}


	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}

	void load(Struct self, FlatStream is) {
		final String base = self.getBase();
		if(!base.isEmpty()) {
			load(Struct.get(base), is);
		}
		for(Field f : self.getFields()) {
            try {
                values.add(Type.create(this, f, is));
            } catch (RuntimeException e) {
                if(host == null) {
                    printStacks(f);
                }
                throw e;
            }
		}
	}

	private void printStacks(Field field) {
        System.out.println();
        System.out.println("=================err data=====================");
        System.out.println("error in read field:" + field.getName());
        System.out.println(this);
        System.out.println("=================err data=====================");
    }

	private void load(Struct self, Element ele) {
		final String base = self.getBase();
		if(!base.isEmpty()) {
			load(Struct.get(base), ele);
		}
		for(Field f : self.getFields()) {
		    try {
                final String fname = f.getName();
                List<Element> ns = Utils.getChildsByTagName(ele, fname);
                if (ns.isEmpty()) {
                    // 如果是简单类型,子element找不到时,尝试从attribue时找
                    // type 属性被保留作 多态类的类名,唯一例外.
                    if ((f.isRaw() || f.isEnum()) && !f.getName().equals("type") && ele.hasAttribute(fname)) {
                        values.add(Type.create(this, f, ele.getAttribute(fname)));
                    } else {
                        Utils.error("type:%s field:%s missing", self.getName(), fname);
                    }
                } else if (ns.size() > 1) {
                    Utils.error("type:%s field:%s duplicate", self.getName(), fname);
                } else {
                    values.add(Type.create(this, f, ns.get(0)));
                }
            } catch (RuntimeException e) {
                if(host == null) {
                    printStacks(f);
                }
                throw e;
            }
		}
	}

	private void load(Struct self, LuaTable g) {
		final String base = self.getBase();
		if(!base.isEmpty()) {
			load(Struct.get(base), g);
		}
		for(Field f : self.getFields()) {
			try {
				final String fname = f.getName();
				LuaValue v = g.get(fname);
				if(v.isnil())
					Utils.error("type:%s field:%s missing", self.getName(), fname);
				values.add(Type.create(this, f, v));
			}  catch (RuntimeException e) {
				if(host == null) {
					printStacks(f);
				}
				throw e;
			}
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("struct<").append(type).append(">{");
		values.forEach(v -> sb.append(v.getDefine().getName()).append(":").append(v).append(","));
		sb.append("}");
		return sb.toString();
	}


	@Override
	public void verifyData() {
		values.forEach(v -> v.verifyData());
	}

	@Override
	public boolean isNull() {
		return false;
	}

}
