package configgen.data;

import configgen.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public final class DataMarshal {
	private final List<String> line = new ArrayList<>();
	
	private DataMarshal put(String x) {
		line.add(x);
		return this;
	}
	
	public DataMarshal putBool(boolean x) {
		return put(x ? "true" : "false");
	}
	
	public DataMarshal putInt(int x) {
		return put(Integer.toString(x));
	}
	
	public DataMarshal putLong(long x) {
		return put(Long.toString(x));
	}
	
	public DataMarshal putFloat(double x) {
		final long lx = (long)x;
		final String s;
		if(lx == x)
			s = Long.toString(lx);
		else {
            double y = x;
            for(int i = 1 ; i < 8; i++) {
                y *= 10;
                if(Math.abs(y - Math.round(y)) < 1.0e-7) {
                    return put(String.format("%." + i + "f", x));
                }
            }
            s = String.format("%f", x);
        }

		return put(s);
	}
	
	public DataMarshal putString(String x) {
		return put(x.replace("\r\n", Main.magicStringForNewLine).replace("\n\r", Main.magicStringForNewLine)
				.replace("\r", Main.magicStringForNewLine).replace("\n", Main.magicStringForNewLine));
	}
	
	public String toData() {
		final ArrayList<String> newLines = new ArrayList<String>(line);
		newLines.add("");
		return newLines.stream().collect(Collectors.joining("\n"));
	}
}
