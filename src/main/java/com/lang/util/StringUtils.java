package com.lang.util;

import java.util.Map;

public class StringUtils {

    public static String replace(String string, Map values) {
        StringBuilder result = new StringBuilder(string.length());

        for (int i = 0; i < string.length();) {
            char c = string.charAt(i);

            if (c == '@' && i + 1 < string.length()) {
                int start = i + 1;
                int end = start;

                char first = string.charAt(start);
                if (Character.isLetter(first) || first == '_') {
                    end++;

                    while (end < string.length()) {
                        char ch = string.charAt(end);
                        if (!Character.isLetterOrDigit(ch) && ch != '_') {
                            break;
                        }
                        end++;
                    }

                    if (end > start) {
                        String name = string.substring(start, end);
                        Object value = values.get(name);

                        if (value != null) {
                            result.append(value);
                            i = end;
                            continue;
                        }
                    }
                }
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

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
}