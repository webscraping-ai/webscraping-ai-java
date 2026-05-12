package ai.webscraping;

import java.time.Duration;

/**
 * Immutable configuration for a {@link Client}.
 *
 * <p>Build with {@link #builder()} or use {@link #fromEnvironment()} to pick up
 * the API key from the {@code WEBSCRAPING_AI_API_KEY} environment variable.
 *
 * <p>{@link #getRequestTimeout() Request timeout} is the fallback per-request
 * deadline used by the default transport. Pass a non-positive {@link Duration}
 * to disable the implicit timeout entirely (callers manage it themselves).
 */
public final class Config {

    /** Production API base URL. */
    public static final String DEFAULT_BASE_URL = "https://api.webscraping.ai";

    /** Default fallback per-request timeout (60 seconds). */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /** Environment variable consulted when {@link Builder#apiKey(String)} is not set. */
    public static final String API_KEY_ENV = "WEBSCRAPING_AI_API_KEY";

    private final String apiKey;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final Transport transport;
    private final String userAgent;

    private Config(Builder b) {
        String apiKey = b.apiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv(API_KEY_ENV);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "API key is required: set Config.Builder.apiKey(...) or the "
                    + API_KEY_ENV + " environment variable.");
        }
        this.apiKey = apiKey;

        String baseUrl = b.baseUrl == null || b.baseUrl.isEmpty() ? DEFAULT_BASE_URL : b.baseUrl;
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;

        this.requestTimeout = b.requestTimeout != null ? b.requestTimeout : DEFAULT_REQUEST_TIMEOUT;
        this.transport = b.transport != null ? b.transport : new JdkHttpTransport();
        this.userAgent = b.userAgent != null ? b.userAgent : "webscraping-ai-java/" + Version.VERSION;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Convenience constructor — reads the API key from the env var only. */
    public static Config fromEnvironment() {
        return new Builder().build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Transport getTransport() {
        return transport;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public static final class Builder {
        private String apiKey;
        private String baseUrl;
        private Duration requestTimeout;
        private Transport transport;
        private String userAgent;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
