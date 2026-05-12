package ai.webscraping.exception;

/**
 * Thrown when the API returns a non-2xx HTTP response. Carries the HTTP status
 * plus the API's documented error envelope ({@code message}, {@code status_code},
 * {@code status_message}, {@code body}). The last three are populated when the
 * API surfaces a target-page error as a 5xx.
 *
 * Catch this to handle any HTTP response error; catch a per-status subclass
 * ({@link BadRequestException}, {@link RateLimitException}, etc.) to handle a
 * specific status code.
 */
public class ApiException extends WebScrapingAIException {
    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final Integer statusCode;
    private final String statusMessage;
    private final String body;
    private final String responseBody;

    public ApiException(int httpStatus, String message, Integer statusCode, String statusMessage,
                        String body, String responseBody) {
        super(buildMessage(httpStatus, message));
        this.httpStatus = httpStatus;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;
        this.responseBody = responseBody;
    }

    private static String buildMessage(int httpStatus, String message) {
        if (message == null || message.isEmpty()) {
            return "HTTP " + httpStatus;
        }
        return "HTTP " + httpStatus + ": " + message;
    }

    /** Maps an HTTP status to the matching per-status subclass; falls back to ApiException. */
    public static ApiException forStatus(int httpStatus, String message, Integer statusCode,
                                         String statusMessage, String body, String responseBody) {
        switch (httpStatus) {
            case 400:
                return new BadRequestException(message, statusCode, statusMessage, body, responseBody);
            case 402:
                return new PaymentRequiredException(message, statusCode, statusMessage, body, responseBody);
            case 403:
                return new AuthenticationException(message, statusCode, statusMessage, body, responseBody);
            case 429:
                return new RateLimitException(message, statusCode, statusMessage, body, responseBody);
            case 500:
                return new ServerException(message, statusCode, statusMessage, body, responseBody);
            case 504:
                return new GatewayTimeoutException(message, statusCode, statusMessage, body, responseBody);
            default:
                return new ApiException(httpStatus, message, statusCode, statusMessage, body, responseBody);
        }
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Application-level status code from the API error envelope, or {@code null}. */
    public Integer getStatusCode() {
        return statusCode;
    }

    /** Application-level status message from the API error envelope, or {@code null}. */
    public String getStatusMessage() {
        return statusMessage;
    }

    /** {@code body} field from the error envelope (target-page error body), or {@code null}. */
    public String getBody() {
        return body;
    }

    /** Raw HTTP response body. */
    public String getResponseBody() {
        return responseBody;
    }
}
