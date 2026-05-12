package ai.webscraping.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionHierarchyTest {

    @Test
    void allApiSubclassesExtendApiExceptionAndWebScrapingAIException() {
        ApiException e = new BadRequestException("bad", null, null, null, "{}");
        assertThat(e).isInstanceOf(ApiException.class);
        assertThat(e).isInstanceOf(WebScrapingAIException.class);
        assertThat(e).isInstanceOf(RuntimeException.class);
    }

    @Test
    void forStatusMaps400() {
        ApiException e = ApiException.forStatus(400, "bad", null, null, null, "{}");
        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getHttpStatus()).isEqualTo(400);
    }

    @Test
    void forStatusMaps402() {
        ApiException e = ApiException.forStatus(402, "no credits", null, null, null, "{}");
        assertThat(e).isInstanceOf(PaymentRequiredException.class);
        assertThat(e.getHttpStatus()).isEqualTo(402);
    }

    @Test
    void forStatusMaps403() {
        ApiException e = ApiException.forStatus(403, "bad key", null, null, null, "{}");
        assertThat(e).isInstanceOf(AuthenticationException.class);
        assertThat(e.getHttpStatus()).isEqualTo(403);
    }

    @Test
    void forStatusMaps429() {
        ApiException e = ApiException.forStatus(429, "slow down", null, null, null, "{}");
        assertThat(e).isInstanceOf(RateLimitException.class);
        assertThat(e.getHttpStatus()).isEqualTo(429);
    }

    @Test
    void forStatusMaps500() {
        ApiException e = ApiException.forStatus(500, "boom", 502, "Bad Gateway", "<html>...</html>", "{}");
        assertThat(e).isInstanceOf(ServerException.class);
        assertThat(e.getStatusCode()).isEqualTo(502);
        assertThat(e.getStatusMessage()).isEqualTo("Bad Gateway");
        assertThat(e.getBody()).isEqualTo("<html>...</html>");
    }

    @Test
    void forStatusMaps504() {
        ApiException e = ApiException.forStatus(504, "timeout", null, null, null, "{}");
        assertThat(e).isInstanceOf(GatewayTimeoutException.class);
        assertThat(e.getHttpStatus()).isEqualTo(504);
    }

    @Test
    void forStatusFallsBackToApiExceptionForUnmappedCodes() {
        ApiException e = ApiException.forStatus(418, "teapot", null, null, null, "{}");
        assertThat(e.getClass()).isEqualTo(ApiException.class);
        assertThat(e.getHttpStatus()).isEqualTo(418);
    }

    @Test
    void transportExceptionsExtendBaseButNotApiException() {
        ApiTimeoutException t = new ApiTimeoutException("timed out", new RuntimeException("x"));
        ApiConnectionException c = new ApiConnectionException("refused", new RuntimeException("x"));
        assertThat(t).isInstanceOf(WebScrapingAIException.class);
        assertThat(c).isInstanceOf(WebScrapingAIException.class);
        assertThat(t).isNotInstanceOf(ApiException.class);
        assertThat(c).isNotInstanceOf(ApiException.class);
        assertThat(t.getCause()).hasMessage("x");
    }

    @Test
    void messageIncludesHttpStatus() {
        ApiException e = ApiException.forStatus(429, "Too Many Requests", null, null, null, "{}");
        assertThat(e.getMessage()).isEqualTo("HTTP 429: Too Many Requests");
    }
}
