package com.lang.util;

public class StringUtils {

    public static String unescape(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);

                switch (next) {
                    case 'n':
                        result.append('\n');
                        i++;
                        break;
                    case 'r':
                        result.append('\r');
                        i++;
                        break;
                    case 't':
                        result.append('\t');
                        i++;
                        break;
                    case '\\':
                        result.append('\\');
                        i++;
                        break;
                    case '"':
                        result.append('"');
                        i++;
                        break;
                    default:
                        result.append(c);
                        break;
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static String stripQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }

        return str;
    }

    public static String stripTripleQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.startsWith("\"\"\"") && str.endsWith("\"\"\"")) {
            return str.substring(3, str.length() - 3);
        }

        return str;
    }
}