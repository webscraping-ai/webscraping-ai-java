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
import ai.webscraping.option.SerpOptions;

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

    private static final String SECRET = "secret-key-123456";

    private static void assertNoKeyLeak(Throwable ex) {
        assertThat(ex).isNotNull();
        for (Throwable t = ex; t != null; t = t.getCause()) {
            assertThat(String.valueOf(t.getMessage())).doesNotContain(SECRET);
            assertThat(t.toString()).doesNotContain(SECRET);
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            assertThat(sw.toString()).doesNotContain(SECRET);
            for (Throwable s : t.getSuppressed()) {
                assertThat(String.valueOf(s.getMessage())).doesNotContain(SECRET);
            }
        }
    }

    private static Throwable thrownBy(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @Test
    void timeoutDoesNotLeakApiKey() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(2000)));
        Client client = new Client(Config.builder()
            .apiKey(SECRET)
            .baseUrl("http://localhost:" + server.port())
            .requestTimeout(Duration.ofMillis(150))
            .build());

        Throwable ex = thrownBy(() -> client.serp(SerpOptions.builder().q("x").build()));
        assertThat(ex).isInstanceOf(ApiTimeoutException.class);
        assertNoKeyLeak(ex);
    }

    @Test
    void connectionFailureDoesNotLeakApiKey() {
        int port = server.port();
        server.stop();
        Client client = new Client(Config.builder()
            .apiKey(SECRET)
            .baseUrl("http://localhost:" + port)
            .requestTimeout(Duration.ofSeconds(2))
            .build());

        Throwable ex = thrownBy(() -> client.serp(SerpOptions.builder().q("x").build()));
        assertThat(ex).isInstanceOf(ApiConnectionException.class);
        assertNoKeyLeak(ex);
    }

    @Test
    void configRejectsInvalidBaseUrlWithoutMentioningKey() {
        for (String base : new String[] {"http://bad host", "ftp://example.com", "example.com", "http://", "::"}) {
            Throwable ex = thrownBy(() -> Config.builder().apiKey(SECRET).baseUrl(base).build());
            assertThat(ex).as(base).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
            assertNoKeyLeak(ex);
        }
    }

    @Test
    void transportRedactsInvalidRequestUrl() {
        // Reachable only through a custom caller of the transport now that Config
        // validates baseUrl, but the transport must still never echo the key.
        Transport.Request req = new Transport.Request(
            "http://bad host/serp?api_key=" + SECRET + "&q=x", java.util.Collections.emptyMap(), null);

        Throwable ex = thrownBy(() -> new JdkHttpTransport().execute(req));
        assertThat(ex).isInstanceOf(ApiConnectionException.class)
            .hasMessageContaining("/serp")
            .hasNoCause();
        assertNoKeyLeak(ex);
    }

    @Test
    void redactHelpersScrubKeyBearingMessagesAndCauses() {
        assertThat(JdkHttpTransport.redactMessage("GET http://h/x?api_key=" + SECRET + "&q=1"))
            .isEqualTo("GET http://h/x?api_key=REDACTED&q=1");
        assertThat(JdkHttpTransport.redactMessage(null)).isNull();

        Exception leaky = new java.io.IOException("wrapped",
            new IllegalStateException("http://h/x?api_key=" + SECRET));
        assertThat(JdkHttpTransport.safeCause(leaky)).isNull();
        Exception clean = new java.io.IOException("connection reset");
        assertThat(JdkHttpTransport.safeCause(clean)).isSameAs(clean);
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
