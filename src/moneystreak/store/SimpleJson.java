package moneystreak.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 외부 라이브러리 없이 자체 구현한 최소한의 JSON 읽기/쓰기 유틸리티.
 * 이 프로젝트에서 다루는 데이터(거래 목록, 예산, 스트릭 상태, 그룹 랭킹)를
 * 저장/로드하고, 그룹 경쟁 기능(F9)의 export/import 파일 형식으로 쓰인다.
 *
 * 지원하는 값 타입: Map(JSON object), List(JSON array), String, Number
 * (Long/Double), Boolean, null.
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    // ---------- 쓰기 (Object -> JSON 문자열) ----------

    /** 주어진 값을 들여쓰기가 적용된(사람이 읽기 좋은) JSON 문자열로 변환한다. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder sb, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject((Map<String, Object>) value, sb, indent);
        } else if (value instanceof List) {
            writeArray((List<Object>) value, sb, indent);
        } else if (value instanceof String) {
            writeString((String) value, sb);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value.toString());
        } else {
            // 알 수 없는 타입은 문자열로 취급
            writeString(value.toString(), sb);
        }
    }

    private static void writeObject(Map<String, Object> map, StringBuilder sb, int indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        int n = map.size();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            indent(sb, indent + 1);
            writeString(e.getKey(), sb);
            sb.append(": ");
            writeValue(e.getValue(), sb, indent + 1);
            if (++i < n) sb.append(",");
            sb.append("\n");
        }
        indent(sb, indent);
        sb.append("}");
    }

    private static void writeArray(List<Object> list, StringBuilder sb, int indent) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(sb, indent + 1);
            writeValue(list.get(i), sb, indent + 1);
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        indent(sb, indent);
        sb.append("]");
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("  ");
    }

    // ---------- 읽기 (JSON 문자열 -> Object) ----------

    /**
     * JSON 문자열을 파싱한다.
     * 객체는 LinkedHashMap&lt;String,Object&gt;, 배열은 List&lt;Object&gt;로 반환된다.
     */
    public static Object parse(String json) {
        Parser p = new Parser(json);
        p.skipWhitespace();
        Object result = p.parseValue();
        p.skipWhitespace();
        if (!p.isAtEnd()) {
            throw new IllegalArgumentException("JSON 파싱 오류: 문자열 끝에 불필요한 내용이 있음 (위치 " + p.pos + ")");
        }
        return result;
    }

    private static class Parser {
        private final String s;
        private int pos = 0;

        Parser(String s) {
            this.s = s;
        }

        boolean isAtEnd() {
            return pos >= s.length();
        }

        char peek() {
            return s.charAt(pos);
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) throw error("예상치 못한 입력 끝");
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        Map<String, Object> parseObject() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (!isAtEnd() && peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (isAtEnd()) throw error("객체가 닫히지 않음");
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    break;
                } else {
                    throw error("객체 안에서 ',' 또는 '}' 가 필요함");
                }
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (!isAtEnd() && peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (isAtEnd()) throw error("배열이 닫히지 않음");
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    break;
                } else {
                    throw error("배열 안에서 ',' 또는 ']' 가 필요함");
                }
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (isAtEnd()) throw error("문자열이 닫히지 않음");
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    if (isAtEnd()) throw error("잘못된 이스케이프");
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default:
                            throw error("알 수 없는 이스케이프 문자: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw error("true/false 파싱 실패");
        }

        Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw error("null 파싱 실패");
        }

        Number parseNumber() {
            int start = pos;
            if (!isAtEnd() && peek() == '-') pos++;
            while (!isAtEnd() && Character.isDigit(peek())) pos++;
            boolean isFloat = false;
            if (!isAtEnd() && peek() == '.') {
                isFloat = true;
                pos++;
                while (!isAtEnd() && Character.isDigit(peek())) pos++;
            }
            if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
                isFloat = true;
                pos++;
                if (!isAtEnd() && (peek() == '+' || peek() == '-')) pos++;
                while (!isAtEnd() && Character.isDigit(peek())) pos++;
            }
            String numStr = s.substring(start, pos);
            if (numStr.isEmpty() || numStr.equals("-")) throw error("숫자 파싱 실패");
            if (isFloat) {
                return Double.parseDouble(numStr);
            } else {
                try {
                    return Long.parseLong(numStr);
                } catch (NumberFormatException ex) {
                    return Double.parseDouble(numStr);
                }
            }
        }

        void expect(char c) {
            if (isAtEnd() || peek() != c) {
                throw error("'" + c + "' 가 필요함");
            }
            pos++;
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException("JSON 파싱 오류(위치 " + pos + "): " + message);
        }
    }
}
