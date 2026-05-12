package ai.webscraping.exception;

/** HTTP 429 — too many concurrent requests. */
public class RateLimitException extends ApiException {
    private static final long serialVersionUID = 1L;

    public RateLimitException(String message, Integer statusCode, String statusMessage,
                              String body, String responseBody) {
        super(429, message, statusCode, statusMessage, body, responseBody);
    }
}
