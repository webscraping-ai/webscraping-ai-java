/*
 * Custom query-string encoder for the WebScraping.AI API.
 *
 * The API mixes three encoding conventions in one request:
 *   - headers, fields    -> deepObject + explode  (headers[Cookie]=abc)
 *   - selectors          -> form + explode WITHOUT brackets (selectors=h1&selectors=p)
 *   - everything else    -> flat key=value, booleans serialized as "true"/"false"
 *
 * None of the off-the-shelf encoders (URLEncoder, URI builders) gets all three
 * right in combination, so we own it. See lore/clients.md for the full story.
 */
package ai.webscraping.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class QueryEncoder {

    private final LinkedHashMap<String, Object> entries = new LinkedHashMap<>();

    public QueryEncoder set(String key, Object value) {
        Objects.requireNonNull(key, "key");
        boolean drop = value == null
            || (value instanceof List<?> && ((List<?>) value).isEmpty())
            || (value instanceof Map<?, ?> && ((Map<?, ?>) value).isEmpty());
        if (drop) {
            entries.remove(key);
            return this;
        }
        // LinkedHashMap.put on an existing key replaces the value without
        // changing iteration position — exactly what we want for "replace in
        // place". New keys are appended.
        entries.put(key, value);
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Returns the flattened wire pairs (in declaration order). */
    public List<String[]> pairs() {
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            flatten(entry.getKey(), entry.getValue(), out);
        }
        return out;
    }

    /** Returns the encoded query string with no leading "?". Spaces become %20. */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String[] kv : pairs()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(percent(kv[0])).append('=').append(percent(kv[1]));
        }
        return sb.toString();
    }

    private static void flatten(String key, Object value, List<String[]> out) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            List<String> subKeys = new ArrayList<>(map.size());
            for (Object k : map.keySet()) {
                subKeys.add(String.valueOf(k));
            }
            Collections.sort(subKeys);
            for (String sub : subKeys) {
                Object v = map.get(sub);
                if (v == null) {
                    continue;
                }
                if (v instanceof Map<?, ?> || v instanceof List<?>) {
                    throw new IllegalArgumentException(
                        "QueryEncoder: nested map/list values are not supported (key=" + key + "[" + sub + "])");
                }
                out.add(new String[] { key + "[" + sub + "]", stringify(v) });
            }
            return;
        }
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item == null) {
                    continue;
                }
                if (item instanceof Map<?, ?> || item instanceof List<?>) {
                    throw new IllegalArgumentException(
                        "QueryEncoder: nested map/list values are not supported (key=" + key + ")");
                }
                out.add(new String[] { key, stringify(item) });
            }
            return;
        }
        out.add(new String[] { key, stringify(value) });
    }

    private static String stringify(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        }
        if (value instanceof String || value instanceof Number || value instanceof Character) {
            return value.toString();
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw new IllegalArgumentException(
            "QueryEncoder: unsupported value type " + value.getClass().getName());
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static String percent(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte raw : bytes) {
            int b = raw & 0xFF;
            if ((b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || (b >= '0' && b <= '9')
                || b == '-' || b == '_' || b == '.' || b == '~') {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(HEX[(b >>> 4) & 0xF]);
                sb.append(HEX[b & 0xF]);
            }
        }
        return sb.toString();
    }
}
