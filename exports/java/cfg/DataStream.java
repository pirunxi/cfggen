package cfg;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public final class DataStream {
	private static final String magicStringForNewLine = ".g9~/";
	private final List<String> lines;
	private int index;
	
	public DataStream(List<String> data) {
		lines = data;
		index = 0;
	}
	
	private String getNext() {
		return index < lines.size() ? lines.get(index++) : null;
	}
	
	private void error(String err) {
		throw new RuntimeException(String.format("%d %s", index, err));
	}
	
	private String getNextAndCheckNotEmpty() {
		final String s = getNext();
		if(s == null) 
			error("read not enough");
		return s;
	}
	
	public boolean getBool() {
		final String s = getNextAndCheckNotEmpty().toLowerCase();
		switch (s) {
			case "true":
				return true;
			case "false":
				return false;
			default:
				error(s + " isn't bool");
				break;
		}
		return false;
	}
	
	public int getInt() {
		final String s = getNextAndCheckNotEmpty();
		return Integer.parseInt(s);
	}
	
	public long getLong() {
		final String s = getNextAndCheckNotEmpty();
		return Long.parseLong(s);
	}
	
	public float getFloat() {
		final String s = getNextAndCheckNotEmpty();
		return Float.parseFloat(s);
	}

	public String getString() {
		return getNextAndCheckNotEmpty().replace(magicStringForNewLine, "\n");
	}

	public cfg.CfgObject getObject(String name) {
		try {
			return (cfg.CfgObject)Class.forName(name).getConstructor(cfg.DataStream.class).newInstance(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static DataStream create(String dataFile, String inputEncoding) {
		try {
			return new DataStream(Files.readAllLines(new File(dataFile).toPath(), Charset.forName(inputEncoding)));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("data file:" + dataFile + " loads fail!");
		}
	}
}
