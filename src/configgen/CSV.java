package configgen;

import java.io.*;
import java.util.*;

public final class CSV {

    private static final char comma = ',';
    private static final char quote = '"';
    private static final char cr = '\r';
    private static final char lf = '\n';

    private enum State {
        START, NOQUOTE, QUOTE, QUOTE2, CR,
    }

    public static List<List<String>> parse(Reader reader) throws IOException {
        List<List<String>> result = new ArrayList<>();
        List<String> record = new ArrayList<>();
        State state = State.START;
        StringBuilder field = null;

        for (int i = reader.read(); i != -1; i = reader.read()) {
            char c = (char) i;

            switch (state) {
                case START:
                    switch (c) {
                        case comma:
                            record.add("");
                            break;
                        case quote:
                            field = new StringBuilder();
                            state = State.QUOTE;
                            break;
                        case cr:
                            field = new StringBuilder();
                            state = State.CR;
                            break;
                        default:
                            field = new StringBuilder();
                            field.append(c);
                            state = State.NOQUOTE;
                            break;
                    }
                    break;

                case NOQUOTE:
                    switch (c) {
                        case comma:
                            record.add(field.toString());
                            state = State.START;
                            break;
                        case cr:
                            state = State.CR;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE:
                    switch (c) {
                        case quote:
                            state = State.QUOTE2;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE2:
                    switch (c) {
                        case comma:
                            record.add(field.toString());
                            state = State.START;
                            break;
                        case quote:
                            field.append(quote);
                            state = State.QUOTE;
                            break;
                        case cr:
                            state = State.CR;
                            break;
                        default:
                            field.append(c);
                            state = State.NOQUOTE;
                            break;
                    }
                    break;

                case CR:
                    switch (c) {
                        case comma:
                            field.append(cr);
                            record.add(field.toString());
                            state = State.START;
                            break;
                        case lf:
                            record.add(field.toString());
                            result.add(record);
                            record = new ArrayList<>();
                            state = State.START;
                            break;
                        default:
                            field.append(cr);
                            field.append(c);
                            state = State.NOQUOTE;
                            break;
                    }
                    break;
            }
        }

        switch (state) {
            case START:
                if (!record.isEmpty()) {
                    record.add("");
                    result.add(record);
                }
                break;
            case CR:
                field.append(cr);
            default:
                record.add(field.toString());
                result.add(record);
                break;
        }

        List<List<String>> filtered = new ArrayList<>();
        for (List<String> list : result) {
            boolean allempty = true;
            for (String s : list) {
                if (!s.isEmpty()) {
                    allempty = false;
                    break;
                }
            }
            if (!allempty)
                filtered.add(list);
        }
        return filtered;
    }

    private static final char semicolon = ';';

    private enum ListState {
        START, NOQUOTE, QUOTE, QUOTE2
    }

    public static List<String> parseList(String str) {
        ListState state = ListState.START;
        List<String> list = new ArrayList<>();
        StringBuilder field = null;

        for (char c : str.toCharArray()) {
            switch (state) {
                case START:
                    switch (c) {
                        case semicolon:
                            list.add("");
                            break;
                        case quote:
                            field = new StringBuilder();
                            state = ListState.QUOTE;
                            break;
                        default:
                            field = new StringBuilder();
                            field.append(c);
                            state = ListState.NOQUOTE;
                            break;
                    }
                    break;

                case NOQUOTE:
                    switch (c) {
                        case semicolon:
                            list.add(field.toString());
                            state = ListState.START;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE:
                    switch (c) {
                        case quote:
                            state = ListState.QUOTE2;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE2:
                    switch (c) {
                        case semicolon:
                            list.add(field.toString());
                            state = ListState.START;
                            break;
                        case quote:
                            field.append(quote);
                            state = ListState.QUOTE;
                            break;
                        default:
                            field.append(c);
                            state = ListState.NOQUOTE;
                            break;
                    }
                    break;
            }
        }

        switch (state) {
            case START:
                break;
            default:
                list.add(field.toString());
                break;
        }

        return list;
    }
}
