package ai.webscraping.exception;

/**
 * Base unchecked exception for everything thrown by the WebScraping.AI SDK.
 * Lets callers write a single {@code catch (WebScrapingAIException e)} for any
 * SDK-originated failure without enumerating the per-status / transport
 * subtypes.
 */
public class WebScrapingAIException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WebScrapingAIException(String message) {
        super(message);
    }

    public WebScrapingAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
