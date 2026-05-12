package ai.webscraping.exception;

/** HTTP 504 — target page took too long to respond. */
public class GatewayTimeoutException extends ApiException {
    private static final long serialVersionUID = 1L;

    public GatewayTimeoutException(String message, Integer statusCode, String statusMessage,
                                   String body, String responseBody) {
        super(504, message, statusCode, statusMessage, body, responseBody);
    }
}
