package ai.webscraping.exception;

/** HTTP 403 — missing or invalid API key. */
public class AuthenticationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message, Integer statusCode, String statusMessage,
                                   String body, String responseBody) {
        super(403, message, statusCode, statusMessage, body, responseBody);
    }
}
