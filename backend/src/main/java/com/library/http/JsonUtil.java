package com.library.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled JSON reader/writer. The project intentionally avoids a
 * third-party JSON library so the backend has zero external dependencies
 * and can be built with nothing but the JDK.
 */
public final class JsonUtil {
    private JsonUtil() {
    }

    // ---------- writing ----------

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            writeString(s, out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                writeValue(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeValue(item, out);
            }
            out.append(']');
        } else {
            writeString(value.toString(), out);
        }
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ---------- reading ----------

    /** Parses a JSON object into a Map. Values are String, Double, Boolean, null, List, or Map. */
    public static Map<String, Object> parseObject(String json) {
        Parser parser = new Parser(json);
        Object result = parser.parseValue();
        if (!(result instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        return map;
    }

    private static final class Parser {
        private final String json;
        private int pos;

        Parser(String json) {
            this.json = json == null ? "" : json;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input");
            }
            char c = json.charAt(pos);
            return switch (c) {
                case '{' -> parseObjectValue();
                case '[' -> parseArrayValue();
                case '"' -> parseStringValue();
                case 't', 'f' -> parseBooleanValue();
                case 'n' -> parseNullValue();
                default -> parseNumberValue();
            };
        }

        private Map<String, Object> parseObjectValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseStringValue();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char next = json.charAt(pos++);
                if (next == '}') {
                    break;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
                }
            }
            return map;
        }

        private List<Object> parseArrayValue() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char next = json.charAt(pos++);
                if (next == ']') {
                    break;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
                }
            }
            return list;
        }

        private String parseStringValue() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = json.charAt(pos++);
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char escaped = json.charAt(pos++);
                    switch (escaped) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            String hex = json.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape: \\" + escaped);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBooleanValue() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (json.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        private Object parseNullValue() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        private Double parseNumberValue() {
            int start = pos;
            while (pos < json.length() && "-+.eE0123456789".indexOf(json.charAt(pos)) >= 0) {
                pos++;
            }
            return Double.parseDouble(json.substring(start, pos));
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return json.charAt(pos);
        }

        private void expect(char c) {
            skipWhitespace();
            if (pos >= json.length() || json.charAt(pos) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }
    }
}
