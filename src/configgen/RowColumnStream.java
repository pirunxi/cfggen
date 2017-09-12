package configgen;

import java.util.List;

public final class RowColumnStream extends FlatStream {
	private final List<List<String>> lines;
	private int col;
	private int row;
	
	private static final String EOL = "##"; // %#
	private static final String END = "]]"; // %]
	
	public RowColumnStream(List<List<String>> data) {
		lines = data;
		col = -1;
		row = 0;
	}
	
	private String getNext() {
		while(true) {
			if(row >= lines.size()) return null;
			++col;
			List<String> line = lines.get(row);
			if(col >= line.size()) {
				row++;
				col = -1;
				continue;
			}
			final String data = line.get(col);
			if(data.startsWith(EOL)) {
				row++;
				col = -1;
				continue;
			}
			if(!data.isEmpty()) {
				return data;
			}
		}
	}
	
	@Override
	public boolean isSectionEnd() {
		while(true) {
			if(row >= lines.size()) return true;
			col++;
			List<String> line = lines.get(row);
			if(col >= line.size()) {
				row++;
				col = -1;
				continue;
			}
			final String data = line.get(col);
			if(data.startsWith(EOL)) {
				row++;
				col = -1;
				continue;
			}
			if(!data.isEmpty()) {
				if(data.startsWith(END)) {
					return true;
				} else {
					col--;
					return false;
				}
			}
		}
	}
	
	private void error(String err) {
		throw new RuntimeException(String.format("%d:%d %s%n line:%s", row + 1, col + 1, err,
                (lines.isEmpty() ? "" : lines.get(Math.min(row, lines.size() - 1)))));
	}
	
	private String getNextAndCheckNotEmpty() {
		final String s = getNext();
		if(s == null) 
			error("read not enough");
		return s;
	}
	
	@Override
	public boolean getBool() {
		final String s = getNextAndCheckNotEmpty();
		if(s.equalsIgnoreCase("true"))
			return true;
		else if(s.equalsIgnoreCase("false"))
			return false;
		else 
			error(s + " isn't bool");
		return false;
	}
	
	@Override
	public int getInt() {
		final String s = getNextAndCheckNotEmpty();
		try {
			return Integer.parseInt(s);
		} catch(Exception e) {
			error(s + " isn't int");
			return -1;
		}
	}
	
	@Override
	public long getLong() {
		final String s = getNextAndCheckNotEmpty();
		try {
			return Long.parseLong(s);
		} catch(Exception e) {
			error(s + " isn't long");
			return -1;
		}
	}
	
	@Override
	public float getFloat() {
		final String s = getNextAndCheckNotEmpty();
		try {
			return Float.parseFloat(s);
		} catch(Exception e) {
			error(s + " isn't float");
			return 0f;
		}
	}

	@Override
	public String getString() {
		return getNextAndCheckNotEmpty();
	}
	
	public String toCSVData() {
		StringBuilder sb = new StringBuilder();
		for(List<String> line : lines) {
			for(String s : line) {
				sb.append(s).append(",");
			}
		}
		return sb.toString();
	}


}
