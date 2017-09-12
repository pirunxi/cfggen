package configgen;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by HuangQiang on 2017/1/18.
 */
public class Localized {
    public final static Localized Ins = new Localized();
    private final Map<String, String> mapper = new HashMap<>();
    private final Map<String, String> hasMappers = new HashMap<>();
    private final Set<String> notMappers = new HashSet<>();

    private boolean hasLocalized = false;

    public boolean isHasLocalized() {
        return hasLocalized;
    }

    @SuppressWarnings("unchecked")
    public void load(String file) throws Exception {
        String fullPath = Utils.combine(Main.csvDir, file);
        List<List<String>> lines = (List<List<String>>)Utils.parseAsXmlOrLuaOrFlatStream(fullPath);
        for(List<String> line : lines) {
            line = line.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
            if(line.isEmpty()) continue;
            if(line.get(0).startsWith("##")) continue;
            if(line.size() != 2) {
                Utils.error("localized file. invalid line:%s", line);
            }
            if(mapper.put(unescape(line.get(0)), unescape(line.get(1))) != null) {
                Utils.error("localized file. duplicate line:%s", line);
            }
        }
        hasLocalized = true;
    }

    public String getLocalizedStr(String src) {
        return mapper.get(src);
    }

    public void addNotLocalizedStr(String src) {
        notMappers.add(src);
    }

    public void addHasLocalizedStr(String src, String dst) {
        hasMappers.put(escape(src), escape(dst));
    }

    public static String escape(String s) {
        return s.replace("\n", "$enter$").replace("\"", "$quote$").replace("\'", "$quote2$");
    }

    public static String unescape(String s) {
        return s.replace("$enter$", "\n").replace("$quote$", "\"").replace("$quote2$", "\'");
    }

    public void saveUnLocalizedAs(String file) {
        final String text = notMappers.stream()
                .map(Localized::escape).collect(Collectors.joining("\n"));
        Utils.save(Utils.combine(Main.csvDir, file), text);
    }

    public static String escapeCSV(String s) {
        return s.contains(",") ?  "\"" + s + "\"" : s;
    }

    public void saveLocalizedAs(String file) {
        final String text = hasMappers.entrySet().stream()
                .map(e -> escapeCSV(e.getKey()) + "," + escapeCSV(e.getValue())).collect(Collectors.joining("\n"));
        Utils.save(Utils.combine(Main.csvDir, file), text);
    }
}
