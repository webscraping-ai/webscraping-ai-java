package ai.webscraping.exception;

/** HTTP 400 — malformed request. */
public class BadRequestException extends ApiException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String message, Integer statusCode, String statusMessage,
                               String body, String responseBody) {
        super(400, message, statusCode, statusMessage, body, responseBody);
    }
}
