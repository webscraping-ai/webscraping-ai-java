package ai.webscraping.exception;

/**
 * The request did not receive a complete response within the configured
 * timeout (no HTTP status was observed).
 */
public class ApiTimeoutException extends WebScrapingAIException {
    private static final long serialVersionUID = 1L;

    public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
