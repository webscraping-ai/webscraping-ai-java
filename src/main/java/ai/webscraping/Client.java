package ai.webscraping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import ai.webscraping.exception.ApiException;
import ai.webscraping.internal.Json;
import ai.webscraping.internal.QueryEncoder;
import ai.webscraping.option.CommonOptions;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;

/**
 * Official Java client for the WebScraping.AI API.
 *
 * <p>Typical use:
 * <pre>{@code
 * Client client = new Client(Config.builder().apiKey("YOUR_KEY").build());
 * String html = client.html(HtmlOptions.builder()
 *     .url("https://example.com")
 *     .js(true)
 *     .build());
 * }</pre>
 *
 * <p>All methods are synchronous. Errors are unchecked: any non-2xx response
 * throws a typed {@link ai.webscraping.exception.ApiException} subclass;
 * transport-level failures throw
 * {@link ai.webscraping.exception.ApiTimeoutException} or
 * {@link ai.webscraping.exception.ApiConnectionException}.
 */
public final class Client {

    private final Config config;

    public Client(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config is required");
        }
        this.config = config;
    }

    /** Convenience constructor reading the API key from {@code WEBSCRAPING_AI_API_KEY}. */
    public Client() {
        this(Config.fromEnvironment());
    }

    public Config getConfig() {
        return config;
    }

    // ---------- /html ----------
    public String html(HtmlOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        if (notEmpty(opts.getFormat())) {
            q.set("format", opts.getFormat());
        }
        if (opts.getReturnScriptResult() != null) {
            q.set("return_script_result", opts.getReturnScriptResult());
        }
        return request("/html", q).getBody();
    }

    // ---------- /text ----------
    public String text(TextOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        if (notEmpty(opts.getTextFormat())) {
            q.set("text_format", opts.getTextFormat());
        }
        if (opts.getReturnLinks() != null) {
            q.set("return_links", opts.getReturnLinks());
        }
        return request("/text", q).getBody();
    }

    // ---------- /selected ----------
    public String selected(SelectedOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        require(opts.getSelector(), "opts.selector");
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        q.set("selector", opts.getSelector());
        if (notEmpty(opts.getFormat())) {
            q.set("format", opts.getFormat());
        }
        return request("/selected", q).getBody();
    }

    // ---------- /selected-multiple ----------
    public SelectedMultipleResult selectedMultiple(SelectedMultipleOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        if (opts.getSelectors() == null || opts.getSelectors().isEmpty()) {
            throw new IllegalArgumentException("opts.selectors must contain at least one selector");
        }
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        q.set("selectors", opts.getSelectors());
        String body = request("/selected-multiple", q).getBody();
        List<List<String>> parsed = Json.read(body, new TypeReference<List<List<String>>>() {});
        return new SelectedMultipleResult(parsed);
    }

    // ---------- /ai/question ----------
    public String question(QuestionOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        require(opts.getQuestion(), "opts.question");
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        q.set("question", opts.getQuestion());
        if (notEmpty(opts.getFormat())) {
            q.set("format", opts.getFormat());
        }
        String body = request("/ai/question", q).getBody();
        // The API returns the answer as a JSON-encoded string by default (with
        // wrapping quotes); strip them so callers get the plain text. If
        // format=json is set, the body is an object — leave it alone.
        return unwrapJsonString(body);
    }

    // ---------- /ai/fields ----------
    public FieldsResult fields(FieldsOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        if (opts.getFields() == null || opts.getFields().isEmpty()) {
            throw new IllegalArgumentException("opts.fields must contain at least one field");
        }
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        q.set("fields", opts.getFields());
        String body = request("/ai/fields", q).getBody();
        return Json.read(body, FieldsResult.class);
    }

    // ---------- /account ----------
    public AccountInfo account() {
        QueryEncoder q = new QueryEncoder();
        String body = request("/account", q).getBody();
        return Json.read(body, AccountInfo.class);
    }

    // ---------- internals ----------

    private Transport.Response request(String path, QueryEncoder query) {
        // api_key prepended so it always appears first on the wire (matches
        // every other SDK). We concatenate encoded strings rather than calling
        // set() again because the params may contain repeated keys (selectors)
        // that set() would collapse to the last value.
        String apiKeySegment = new QueryEncoder().set("api_key", config.getApiKey()).encode();
        String rest = query.encode();
        String fullQuery = rest.isEmpty() ? apiKeySegment : apiKeySegment + "&" + rest;

        String url = config.getBaseUrl() + path + "?" + fullQuery;
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", config.getUserAgent());
        headers.put("Accept", "application/json, text/html, */*");

        Transport.Request req = new Transport.Request(url, headers, config.getRequestTimeout());
        Transport.Response resp = config.getTransport().execute(req);

        int status = resp.getStatus();
        if (status >= 200 && status < 300) {
            return resp;
        }
        throw parseApiException(status, resp.getBody());
    }

    private static ApiException parseApiException(int status, String body) {
        ErrorEnvelope env = Json.tryRead(body, ErrorEnvelope.class);
        String message = env != null ? env.message : null;
        Integer statusCode = env != null ? env.statusCode : null;
        String statusMessage = env != null ? env.statusMessage : null;
        String innerBody = env != null ? env.body : null;
        if (message == null) {
            message = body == null || body.isEmpty() ? null : body;
        }
        return ApiException.forStatus(status, message, statusCode, statusMessage, innerBody, body);
    }

    private static QueryEncoder commonParams(CommonOptions c) {
        QueryEncoder q = new QueryEncoder();
        if (c.getHeaders() != null && !c.getHeaders().isEmpty()) {
            q.set("headers", c.getHeaders());
        }
        if (c.getTimeout() != null) {
            q.set("timeout", c.getTimeout());
        }
        if (c.getJs() != null) {
            q.set("js", c.getJs());
        }
        if (c.getJsTimeout() != null) {
            q.set("js_timeout", c.getJsTimeout());
        }
        if (notEmpty(c.getWaitFor())) {
            q.set("wait_for", c.getWaitFor());
        }
        if (notEmpty(c.getProxy())) {
            q.set("proxy", c.getProxy());
        }
        if (notEmpty(c.getCountry())) {
            q.set("country", c.getCountry());
        }
        if (notEmpty(c.getCustomProxy())) {
            q.set("custom_proxy", c.getCustomProxy());
        }
        if (notEmpty(c.getDevice())) {
            q.set("device", c.getDevice());
        }
        if (c.getErrorOn404() != null) {
            q.set("error_on_404", c.getErrorOn404());
        }
        if (c.getErrorOnRedirect() != null) {
            q.set("error_on_redirect", c.getErrorOnRedirect());
        }
        if (notEmpty(c.getJsScript())) {
            q.set("js_script", c.getJsScript());
        }
        return q;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static String unwrapJsonString(String body) {
        if (body == null || body.length() < 2) {
            return body == null ? "" : body;
        }
        if (body.charAt(0) != '"' || body.charAt(body.length() - 1) != '"') {
            return body;
        }
        String unwrapped = Json.tryRead(body, String.class);
        return unwrapped != null ? unwrapped : body;
    }

    /** Internal Jackson DTO for the API's error envelope. */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ErrorEnvelope {
        public String message;
        public Integer statusCode;
        public String statusMessage;
        public String body;

        @com.fasterxml.jackson.annotation.JsonProperty("status_code")
        public void setStatusCodeSnake(Integer v) {
            this.statusCode = v;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("status_message")
        public void setStatusMessageSnake(String v) {
            this.statusMessage = v;
        }
    }
}
