package configgen.data;

import configgen.FlatStream;
import configgen.Main;
import configgen.RowColumnStream;
import configgen.type.Config;
import configgen.type.Field;
import configgen.type.Struct;
import org.luaj.vm2.LuaValue;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashSet;

public abstract class Type {
//	public final static String UNLIMIT_STR = "unlimit";
//	public final static int UNLIMIT_VALUE= -1;
	public final static String NULL_STR = "null";
	public final static int NULL_VALUE = -1;
	
	protected final Field define;
	protected final FStruct host;
	
	public Type(FStruct host, Field define) {
		this.host = host;
		this.define = define;
	}
	
	public final Field getDefine() {
		return define;
	}
	
	public final FStruct getHost() {
		return host;
	}
	
	static void error(String err) {
		throw new RuntimeException(err);
	}
	
	public static Type create(FStruct host, Field define, FlatStream is) {
        if(define.isCompound()) {
            is = new RowColumnStream(Arrays.asList(Arrays.asList(is.getString().split(define.getdelimiter()))));
        }
		final String type = define.getType();
		if(define.isRaw()) {
			if(type.equals("bool")) {
				return new FBool(host, define, is);
			} else if(type.equals("int")) {
				return new FInt(host, define, is);
			} else if(type.equals("long")) {
				return new FLong(host, define, is);
			} else if(type.equals("float")) {
				return new FFloat(host, define, is);
			} else if(type.equals("string")) {
				return new FString(host, define, is);
			}
		} else if(define.isContainer()) {
			if(type.equals("list")) {
				FList d = new FList(host, define);
				d.loadMultiRecordNotCheckEnd(is);
				return d;
			} else if(type.equals("set")) {
				return new FSet(host, define, is);
			} else if(type.equals("map")) {
				return new FMap(host, define, is);
			}
			
		} else if(define.isEnum()) {
			return new FEnum(host, define, is);
		} else if(define.isStruct()) {
			final String baseType = define.getType();
			final Struct base = Struct.get(define.getType());
			if(base.isDynamic()) {
				final String subType = base.getNamespace() + "." + is.getString();
				Struct real = Struct.get(subType);
				if(real == null || !real.getFullName().equals(subType))
					error("dynamic type:" + subType + " unknown");
				if(real.isDynamic())
					error("data type:" + subType + " is dynamic type!");
				if(!Struct.isDeriveFrom(subType, baseType))
					error("dynamic type:" + subType + " isn't sub type of:" + baseType);
				return new FStruct(host, define, subType, is);
			} else {
				return new FStruct(host, define, baseType, is);
			}
		}
		
		error("unknown type:" + type);
		return null;
	}
	
	public static Type create(FStruct host, Field define, String value) {
		final String type = define.getType();
		if(define.isRaw()) {
			if(type.equals("bool")) {
				return new FBool(host, define, value);
			} else if(type.equals("int")) {
				return new FInt(host, define, value);
			} else if(type.equals("long")) {
				return new FLong(host, define, value);
			} else if(type.equals("float")) {
				return new FFloat(host, define, value);
			} else if(type.equals("string")) {
				return new FString(host, define, value);
			}
		} else if(define.isEnum()) {
			return new FEnum(host, define, value);
		} else {
			error("unknown type:" + type);
		}
		return null;
	}
	

	public static Type create(FStruct host, Field define, Element node) {
		final String type = define.getType();
		if(define.isRaw()) {
			if(type.equals("bool")) {
				return new FBool(host, define, node);
			} else if(type.equals("int")) {
				return new FInt(host, define, node);
			} else if(type.equals("long")) {
				return new FLong(host, define, node);
			} else if(type.equals("float")) {
				return new FFloat(host, define, node);
			} else if(type.equals("string")) {
				return new FString(host, define, node);
			}
		} else if(define.isContainer()) {
			if(type.equals("list")) {
				FList d = new FList(host, define);
				d.loadMultiRecord(node);
				return d;
			} else if(type.equals("set")) {
				return new FSet(host, define, node);
			} else if(type.equals("map")) {
				return new FMap(host, define, node);
			}
		} else if(define.isEnum()) {
			return new FEnum(host, define, node);
		} else if(define.isStruct()) {
			final String baseType = define.getType();
			final Struct base = Struct.get(define.getType());
			if(base.isDynamic()) {
				final String subType = base.getNamespace() + "." + node.getAttribute("type");
				Struct real = Struct.get(subType);
				if(real == null || !real.getFullName().equals(subType))
					error("dynamic type:" + subType + " unknown");
				if(real.isDynamic())
					error("data type:" + subType + " is dynamic type!");
				if(!Struct.isDeriveFrom(subType, baseType))
					error("dynamic type:" + subType + " isn't sub type of:" + baseType);
				return new FStruct(host, define, subType, node);
			} else {
				return new FStruct(host, define, baseType, node);
			}
		}
		
		error("unknown type:" + type);
		return null;
	}


	public static Type create(FStruct host, Field define, LuaValue data) {
		final String type = define.getType();
		if(define.isRaw()) {
			if(type.equals("bool")) {
				return new FBool(host, define, data.toboolean());
			} else if(type.equals("int")) {
				return new FInt(host, define, data.toint());
			} else if(type.equals("long")) {
				return new FLong(host, define, data.tolong());
			} else if(type.equals("float")) {
				return new FFloat(host, define, data.todouble());
			} else if(type.equals("string")) {
				return new FString(host, define, data.tojstring());
			}
		} else if(define.isContainer()) {
			if(type.equals("list")) {
				FList d = new FList(host, define);
				d.loadMultiRecord(data);
				return d;
			} else if(type.equals("set")) {
				return new FSet(host, define, data.checktable());
			} else if(type.equals("map")) {
				return new FMap(host, define, data.checktable());
			}
		} else if(define.isEnum()) {
			return new FEnum(host, define, data.tojstring());
		} else if(define.isStruct()) {
			data.checktable();
			final String baseType = define.getType();
			final Struct base = Struct.get(define.getType());
			if(base.isDynamic()) {
				final String subType = base.getNamespace() + "." + data.get("class").tojstring();
				Struct real = Struct.get(subType);
				if(real == null || !real.getFullName().equals(subType))
					error("dynamic type:" + subType + " unknown");
				if(real.isDynamic())
					error("data type:" + subType + " is dynamic type!");
				if(!Struct.isDeriveFrom(subType, baseType))
					error("dynamic type:" + subType + " isn't sub type of:" + baseType);
				return new FStruct(host, define, subType, data.checktable());
			} else {
				return new FStruct(host, define, baseType, data.checktable());
			}
		}

		error("unknown type:" + type);
		return null;
	}
	
	public abstract boolean isNull();
	public abstract void accept(Visitor visitor);

    private static String getKey(FStruct d, Config c) {
        return c.isSingle() ? "0" : d.getField(c.getIndex()).toString();
    }
	
	public static void errorRef(Type value, String refName) {
		System.out.println("config:" + Main.getCurVerifyConfig().getName() + " key:" + getKey((FStruct)Main.getCurVerifyData(), Main.getCurVerifyConfig()) + " struct:" + value.host.getType() + " field:" + value.define.getName() + " value:" + value + " err ref:" + refName);
	}

	public static void verifyData(Type value, String ref) {
		if(value.isNull() || ref.isEmpty()) return;
		if(ref.contains("|")) {
			if(Arrays.asList(ref.split("\\|")).stream().allMatch(refName -> !Config.getData(refName).contains(value)))
				errorRef(value, ref);
		} else if(ref.contains(",")) {
			for(String refName : ref.split(",")) {
				if(!Config.getData(refName).contains(value))
					errorRef(value, refName);
			}
		} else {
			HashSet<Type> validValues = Config.getData(ref);
			if(!validValues.contains(value))
				errorRef(value, ref);
		}
	}

	public void verifyData() {
		verifyData(this, define.getRef());
	}


}
