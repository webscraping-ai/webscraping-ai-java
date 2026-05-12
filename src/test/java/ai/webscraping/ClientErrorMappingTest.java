package ai.webscraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import ai.webscraping.exception.ApiException;
import ai.webscraping.exception.AuthenticationException;
import ai.webscraping.exception.BadRequestException;
import ai.webscraping.exception.GatewayTimeoutException;
import ai.webscraping.exception.PaymentRequiredException;
import ai.webscraping.exception.RateLimitException;
import ai.webscraping.exception.ServerException;
import ai.webscraping.option.HtmlOptions;

class ClientErrorMappingTest {

    private WireMockServer server;
    private Client client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
        client = new Client(Config.builder()
            .apiKey("test-key")
            .baseUrl("http://localhost:" + server.port())
            .build());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private void stubError(int status, String body) {
        stubFor(get(urlPathEqualTo("/html"))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }

    private HtmlOptions opts() {
        return HtmlOptions.builder().url("https://example.com").build();
    }

    @Test
    void status400MapsToBadRequestException() {
        stubError(400, "{\"message\":\"bad url\"}");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(BadRequestException.class)
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("HTTP 400")
            .hasMessageContaining("bad url");
    }

    @Test
    void status402MapsToPaymentRequiredException() {
        stubError(402, "{\"message\":\"no credits\"}");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(PaymentRequiredException.class);
    }

    @Test
    void status403MapsToAuthenticationException() {
        stubError(403, "{\"message\":\"bad key\"}");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void status429MapsToRateLimitException() {
        stubError(429, "{\"message\":\"slow down\"}");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(RateLimitException.class);
    }

    @Test
    void status500MapsToServerExceptionWithEnvelope() {
        stubError(500, "{\"message\":\"target page failed\",\"status_code\":502,"
            + "\"status_message\":\"Bad Gateway\",\"body\":\"<html>err</html>\"}");

        ApiException ex = (ApiException) catchThrowable(() -> client.html(opts()));
        assertThat(ex).isInstanceOf(ServerException.class);
        assertThat(ex.getStatusCode()).isEqualTo(502);
        assertThat(ex.getStatusMessage()).isEqualTo("Bad Gateway");
        assertThat(ex.getBody()).isEqualTo("<html>err</html>");
    }

    @Test
    void status504MapsToGatewayTimeoutException() {
        stubError(504, "{\"message\":\"target timeout\"}");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(GatewayTimeoutException.class);
    }

    @Test
    void nonJsonErrorBodyFallsBackToRawBodyMessage() {
        stubError(400, "not json");
        assertThatThrownBy(() -> client.html(opts()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not json");
    }

    @Test
    void unmappedStatusFallsBackToBaseApiException() {
        stubError(418, "I'm a teapot");
        ApiException ex = (ApiException) catchThrowable(() -> client.html(opts()));
        assertThat(ex.getClass()).isEqualTo(ApiException.class);
        assertThat(ex.getHttpStatus()).isEqualTo(418);
    }

    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
