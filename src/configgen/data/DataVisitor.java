package configgen.data;

import java.util.Map;
import java.util.Set;

import configgen.type.Field;
import configgen.type.Struct;


public class DataVisitor implements Visitor {
	private final Set<String> groups;
	private final DataMarshal fs = new DataMarshal();
	public DataVisitor(Set<String> groups) {
		this.groups = groups;
	}
	
	@Override
	public void accept(FBool x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putBool(x.value);
		}
	}

	@Override
	public void accept(FFloat x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putFloat(x.value);
		}
	}

	@Override
	public void accept(FInt x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putInt(x.value);
		}
	}


	@Override
	public void accept(FLong x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putLong(x.value);
		}
	}
	

	@Override
	public void accept(FString x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putString(x.value);
		}
	}
	

	@Override
	public void accept(FEnum x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putInt(x.value);
		}
		
	}

	@Override
	public void accept(FStruct x) {
		Field define = x.getDefine();
		if(define == null || define.checkInGroup(groups)) {
			if(define != null && Struct.get(define.getType()).isDynamic()) {
				fs.putString(x.getType());
			}
			for(Type field : x.getValues()) {
				field.accept(this);
			}
		}
	}

	@Override
	public void accept(FList x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putInt(x.values.size());
			for(Type field : x.values) {
				field.accept(this);
			}
		}
	}
	
	@Override
	public void accept(FMap x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putInt(x.values.size());
			for(Map.Entry<Type, Type> field : x.values.entrySet()) {
				field.getKey().accept(this);
				field.getValue().accept(this);
			}
		}
	}

	@Override
	public void accept(FSet x) {
		if(x.getDefine().checkInGroup(groups)) {
			fs.putInt(x.values.size());
			for(Type field : x.values) {
				field.accept(this);
			}
		}
	}
	
	public final String toData() {
		return fs.toData();
	}



}
