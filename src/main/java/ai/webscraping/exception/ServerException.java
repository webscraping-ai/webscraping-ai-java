package ai.webscraping.exception;

/** HTTP 500 — server-side error (often a target-page failure surfaced via {@code body}). */
public class ServerException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ServerException(String message, Integer statusCode, String statusMessage,
                           String body, String responseBody) {
        super(500, message, statusCode, statusMessage, body, responseBody);
    }
}
