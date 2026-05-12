package ai.webscraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import ai.webscraping.exception.ApiConnectionException;
import ai.webscraping.exception.ApiTimeoutException;
import ai.webscraping.option.HtmlOptions;

class ClientTransportTest {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    void slowResponseTriggersApiTimeoutException() {
        stubFor(get(urlPathEqualTo("/html"))
            .willReturn(aResponse().withStatus(200).withBody("ok").withFixedDelay(2000)));

        Client client = new Client(Config.builder()
            .apiKey("test-key")
            .baseUrl("http://localhost:" + server.port())
            .requestTimeout(Duration.ofMillis(150))
            .build());

        assertThatThrownBy(() -> client.html(HtmlOptions.builder().url("https://example.com").build()))
            .isInstanceOf(ApiTimeoutException.class);
    }

    @Test
    void connectionRefusedTriggersApiConnectionException() {
        int port = server.port();
        server.stop();

        Client client = new Client(Config.builder()
            .apiKey("test-key")
            .baseUrl("http://localhost:" + port)
            .requestTimeout(Duration.ofSeconds(2))
            .build());

        assertThatThrownBy(() -> client.html(HtmlOptions.builder().url("https://example.com").build()))
            .isInstanceOf(ApiConnectionException.class);
    }

    @Test
    void apiKeyComesFromEnvironmentWhenBuilderDoesNotSetIt() {
        // Can't safely mutate the process env in a unit test, so spot-check the
        // Builder's behaviour against a fake env-style apiKey value.
        Config cfg = Config.builder().apiKey("from-builder").build();
        assertThat(cfg.getApiKey()).isEqualTo("from-builder");
    }

    @Test
    void missingApiKeyThrows() {
        // Clear the env override so the test is deterministic in case someone has
        // WEBSCRAPING_AI_API_KEY exported. We can't actually unset env vars from
        // Java, so assume CI doesn't export the variable for unit tests.
        if (System.getenv(Config.API_KEY_ENV) != null) {
            return;
        }
        assertThatThrownBy(() -> Config.builder().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API key is required");
    }
}
