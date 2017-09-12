package configgen.data;

import configgen.Localized;
import configgen.Main;
import org.w3c.dom.Element;

import configgen.FlatStream;
import configgen.type.Field;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FString extends Type {
	public FString(FStruct host, Field define, String is) {
		super(host, define);
		value = checkLocalized(is);
	}
	

	private final static String EMPTY = "null";
	public FString(FStruct host, Field define, FlatStream is) {
		super(host, define);
		final String s = is.getString();
		// 因为null用来表示空字符串,%n 存在的意义是为了能够在 string 里配出 null.
		value = checkLocalized(s.equals(EMPTY) ? "" :
			(s.indexOf('%') >= 0 ? s.replace("%#", "#").replace("%]", "]").replace("%n", "n") : s));
	}
	
	public FString(FStruct host, Field define, Element node) {
		this(host, define, node.getFirstChild() != null ? node.getFirstChild().getTextContent() : "");
	}

	public final String value;

	public String toString() {
		return "string:'" + value + "'";
	}

	private String checkLocalized(String s) {
		if(!define.isLocalized()) return s;
		Localized loc = Localized.Ins;
		if(loc.isHasLocalized()) {
			if(s.trim().isEmpty()) return s;
			final String r = loc.getLocalizedStr(s);
			if(r != null) {
				loc.addHasLocalizedStr(s, r);
				return r;
			} else {
				loc.addNotLocalizedStr(s);
			}
		}
		// 那些需要本地化的字符串有时候即使相同的串,也要映射到不同的文字。这时候需要一个tag
		// 来区分他们。
		// 原来一个字符串是 xxxyyzz, 加了tag后为 xxxyyzz@name@
		// 如果没有找到本土化映射,会自动帮它脱去尾部的 @...@
		if(s.endsWith("@")) {
			return s.substring(0, s.lastIndexOf('@', s.length() - 2));
		}
		return s;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.accept(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FString)) return false;
		return this.value.equals(((FString)o).value);
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	@Override
	public boolean isNull() {
		return value.isEmpty();
	}

    public String toFinalPath(String path) {
        return (new File(path).isAbsolute() ? path : Main.csvDir + "/" + path).replace("?", value).replace("*", value.toLowerCase());
    }

    @Override
    public void verifyData() {
        super.verifyData();
        final List<String> refPaths = define.getRefPath();
        if(!refPaths.isEmpty() && !isNull()) {
            final List<String> finalRefPaths = refPaths.stream().map(path -> toFinalPath(path)).collect(Collectors.toList());
            if(finalRefPaths.stream().noneMatch(path -> new File(path).exists())) {
                errorRef(this, finalRefPaths.stream().collect(Collectors.joining("] or [", "[", "]")));
            }
        }
    }
}
