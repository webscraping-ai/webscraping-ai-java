package ai.webscraping.exception;

/** HTTP 402 — account out of credits. */
public class PaymentRequiredException extends ApiException {
    private static final long serialVersionUID = 1L;

    public PaymentRequiredException(String message, Integer statusCode, String statusMessage,
                                    String body, String responseBody) {
        super(402, message, statusCode, statusMessage, body, responseBody);
    }
}
