package configgen.tool;

import configgen.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by HuangQiang on 2017/1/3.
 */
public class ExportBroadcast {
    public static void main(String[] argv) throws Exception {
        final String excelFile = argv[0];
        List<List<String>> lines = Utils.parseExcel(excelFile);
        List<String> out = new ArrayList<>();
        out.add("{");
        for(List<String> line : lines) {
            if(line.size() != 4) continue;
            if(line.get(0).startsWith("##")) continue;
            out.add(String.format("{ title = [===[%s]===], content =[===[%s]===], date = [===[%s]===], isnew=%s},",
                    line.get(0), line.get(1), line.get(2), line.get(3).equalsIgnoreCase("true")));
        }
        out.add("}");
        Utils.save("./broadcast.lua", out.stream().collect(Collectors.joining("\n")));
    }
}
