package configgen;

public abstract class FlatStream {
	public abstract boolean isSectionEnd();	
	public abstract boolean getBool();	
	public abstract int getInt();	
	public abstract long getLong();
	public abstract float getFloat();
	public abstract String getString();
}
