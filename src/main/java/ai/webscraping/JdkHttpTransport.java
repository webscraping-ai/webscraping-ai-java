package ai.webscraping;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

import ai.webscraping.exception.ApiConnectionException;
import ai.webscraping.exception.ApiTimeoutException;

/**
 * Default {@link Transport} implementation backed by {@link HttpClient} from
 * {@code java.net.http}. Zero external dependencies.
 *
 * <p>The internal {@code HttpClient} has no timeout of its own; per-request
 * timeouts come from {@link Request#getTimeout()}.
 */
public final class JdkHttpTransport implements Transport {

    private final HttpClient httpClient;

    public JdkHttpTransport() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
    }

    public JdkHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Response execute(Request request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(request.getUrl()))
            .GET();

        Duration timeout = request.getTimeout();
        if (timeout != null && !timeout.isZero() && !timeout.isNegative()) {
            builder.timeout(timeout);
        }

        for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }

        try {
            HttpResponse<String> resp = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
            );
            return new Response(resp.statusCode(), resp.body());
        } catch (HttpTimeoutException e) {
            throw new ApiTimeoutException("Request to " + redactUrl(request.getUrl()) + " timed out", e);
        } catch (IOException e) {
            throw new ApiConnectionException(
                "Request to " + redactUrl(request.getUrl()) + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiConnectionException(
                "Request to " + redactUrl(request.getUrl()) + " was interrupted", e);
        }
    }

    /**
     * Strips the query string from a URL so secrets (e.g. {@code api_key}) carried
     * as query parameters never appear in exception messages or logs. The path is
     * not secret and is preserved.
     */
    private static String redactUrl(String url) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }
}
