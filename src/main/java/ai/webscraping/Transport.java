package ai.webscraping;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import ai.webscraping.exception.ApiConnectionException;
import ai.webscraping.exception.ApiTimeoutException;

/**
 * HTTP transport abstraction. The default implementation
 * ({@link JdkHttpTransport}) wraps the JDK's {@code java.net.http.HttpClient};
 * users can supply their own (e.g., backed by Apache HttpClient 5 or OkHttp)
 * by passing a custom {@code Transport} through {@link Config}.
 *
 * <p>A {@code Transport} implementation is responsible for the wire-level
 * exchange only. It returns the raw {@link Response} regardless of status —
 * the higher-level {@link Client} maps non-2xx responses to the typed
 * {@link ai.webscraping.exception.ApiException} subclasses. Transport-level
 * failures must be reported as {@link ApiTimeoutException} or
 * {@link ApiConnectionException}.
 */
public interface Transport {

    Response execute(Request request);

    final class Request {
        private final String url;
        private final Map<String, String> headers;
        private final Duration timeout;

        public Request(String url, Map<String, String> headers, Duration timeout) {
            this.url = Objects.requireNonNull(url, "url");
            this.headers = headers == null ? Collections.emptyMap() : new LinkedHashMap<>(headers);
            this.timeout = timeout;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, String> getHeaders() {
            return Collections.unmodifiableMap(headers);
        }

        /** Per-request timeout. May be {@code null} or non-positive for "no transport timeout". */
        public Duration getTimeout() {
            return timeout;
        }
    }

    final class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }
    }
}
