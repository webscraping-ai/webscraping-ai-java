package ai.webscraping.internal;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Centralised Jackson helpers. The {@link ObjectMapper} configuration lives
 * here so the rest of the SDK never touches Jackson directly.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {}

    public static <T> T read(String body, Class<T> type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    public static <T> T read(String body, TypeReference<T> type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the parsed value, or {@code null} if the body is not valid JSON
     * for the requested shape. Used for best-effort parsing of the API's error
     * envelope where a non-JSON body should fall back silently.
     */
    public static <T> T tryRead(String body, Class<T> type) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(body, type);
        } catch (IOException e) {
            return null;
        }
    }
}
