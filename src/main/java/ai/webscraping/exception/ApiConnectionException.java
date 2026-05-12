package ai.webscraping.exception;

/**
 * Transport-level failure (DNS, TCP, TLS, etc.) — no HTTP response was
 * received and the cause is not a timeout.
 */
public class ApiConnectionException extends WebScrapingAIException {
    private static final long serialVersionUID = 1L;

    public ApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
