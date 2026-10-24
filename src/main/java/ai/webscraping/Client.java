package ai.webscraping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import ai.webscraping.exception.ApiException;
import ai.webscraping.internal.Json;
import ai.webscraping.internal.QueryEncoder;
import ai.webscraping.option.CommonOptions;
import ai.webscraping.option.DataOptions;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.SerpLocationsOptions;
import ai.webscraping.option.SerpOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.DataResult;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;
import ai.webscraping.result.SerpLocation;
import ai.webscraping.result.SerpResult;

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

    /** Wire names of the typed {@link DataOptions} fields, mapped to their builder methods. */
    private static final Map<String, String> DATA_TYPED_PARAMS = Map.of(
        "country", "country",
        "transcript", "transcript",
        "transcript_language", "transcriptLanguage");

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
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        if (notEmpty(opts.getSelector())) {
            q.set("selector", opts.getSelector());
        }
        if (notEmpty(opts.getFormat())) {
            q.set("format", opts.getFormat());
        }
        return request("/selected", q).getBody();
    }

    // ---------- /selected-multiple ----------
    public SelectedMultipleResult selectedMultiple(SelectedMultipleOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        QueryEncoder q = commonParams(opts);
        q.set("url", opts.getUrl());
        if (opts.getSelectors() != null && !opts.getSelectors().isEmpty()) {
            q.set("selectors", opts.getSelectors());
        }
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

    // ---------- /serp ----------
    /**
     * Parsed search engine results for {@code opts.q}. Billed per results
     * page returned (see https://webscraping.ai/docs#serp); failed searches
     * are not charged. Query-shaped: none of the page-scraping options apply.
     *
     * <p>Every search runs from one fixed location ({@code location},
     * {@code lat} + {@code lon} or {@code uule}, otherwise the capital of the
     * {@code gl} country), so repeats and later pages are consistent.
     * {@code pages} (1-5) fetches consecutive pages as one de-duplicated list.
     *
     * <p>{@code opts.q} must not be blank, {@code opts.page},
     * {@code opts.pages} and {@code opts.radius}, when set, must be at least
     * 1, and {@code opts.lat}/{@code opts.lon} must be finite; all are
     * checked before any request (checking client-side saves the round
     * trip). Upper bounds (page 100, pages 5, radius 1000) and which location
     * options may be combined are checked by the server: a 400, not billed.
     * The query and location are sent exactly as given; lat/lon are sent as
     * plain decimals rounded to 7 fractional digits.
     *
     * @throws IllegalArgumentException if {@code opts.q} is null or blank,
     *     {@code opts.page}, {@code opts.pages} or {@code opts.radius} is
     *     below 1, or {@code opts.lat} or {@code opts.lon} is NaN or infinite
     */
    public SerpResult serp(SerpOptions opts) {
        require(opts, "opts");
        require(opts.getQ(), "opts.q");
        if (opts.getQ().trim().isEmpty()) {
            throw new IllegalArgumentException("opts.q must not be blank");
        }
        if (opts.getPage() != null && opts.getPage() < 1) {
            throw new IllegalArgumentException("opts.page must be >= 1, got " + opts.getPage());
        }
        if (opts.getPages() != null && opts.getPages() < 1) {
            throw new IllegalArgumentException("opts.pages must be >= 1, got " + opts.getPages());
        }
        if (opts.getRadius() != null && opts.getRadius() < 1) {
            throw new IllegalArgumentException("opts.radius must be >= 1, got " + opts.getRadius());
        }
        requireFinite(opts.getLat(), "opts.lat");
        requireFinite(opts.getLon(), "opts.lon");
        QueryEncoder q = new QueryEncoder();
        q.set("q", opts.getQ());
        if (notEmpty(opts.getEngine())) {
            q.set("engine", opts.getEngine());
        }
        if (notEmpty(opts.getGl())) {
            q.set("gl", opts.getGl());
        }
        if (notEmpty(opts.getHl())) {
            q.set("hl", opts.getHl());
        }
        if (opts.getPage() != null) {
            q.set("page", opts.getPage());
        }
        if (opts.getPages() != null) {
            q.set("pages", opts.getPages());
        }
        if (notEmpty(opts.getLocation())) {
            q.set("location", opts.getLocation());
        }
        if (notEmpty(opts.getUule())) {
            q.set("uule", opts.getUule());
        }
        if (opts.getLat() != null) {
            q.set("lat", formatCoordinate(opts.getLat()));
        }
        if (opts.getLon() != null) {
            q.set("lon", formatCoordinate(opts.getLon()));
        }
        if (opts.getRadius() != null) {
            q.set("radius", opts.getRadius());
        }
        String body = request("/serp", q).getBody();
        return Json.read(body, SerpResult.class);
    }

    // ---------- /serp/locations ----------
    /**
     * Looks up names for {@link SerpOptions.Builder#location(String)}: up to
     * 10 matching locations, most populous first. Pass a result's
     * {@link SerpLocation#getCanonicalName()} (or {@link SerpLocation#getId()})
     * as the {@code location}. Matches English names (e.g. {@code petersburg}).
     * Free on the server, which needs no API key for it.
     *
     * @throws IllegalArgumentException if {@code opts} is null or
     *     {@code opts.limit} is below 1 (the server rejects more than 10 with a 400)
     */
    public List<SerpLocation> serpLocations(SerpLocationsOptions opts) {
        require(opts, "opts");
        if (opts.getLimit() != null && opts.getLimit() < 1) {
            throw new IllegalArgumentException("opts.limit must be >= 1, got " + opts.getLimit());
        }
        QueryEncoder q = new QueryEncoder();
        if (notEmpty(opts.getQ())) {
            q.set("q", opts.getQ());
        }
        if (opts.getLimit() != null) {
            q.set("limit", opts.getLimit());
        }
        String body = request("/serp/locations", q).getBody();
        return Json.read(body, new TypeReference<List<SerpLocation>>() {});
    }

    /** The most populous locations; see {@link #serpLocations(SerpLocationsOptions)}. */
    public List<SerpLocation> serpLocations() {
        return serpLocations(SerpLocationsOptions.builder().build());
    }

    // ---------- /data ----------
    /**
     * Structured JSON for a page on a supported site (e.g. a YouTube video, a
     * TikTok profile, an X post, a LinkedIn company, an Instagram reel or a
     * Reddit thread). Priced per site (see https://webscraping.ai/docs#data),
     * including pages that parse empty ({@code parse_failed}) or no longer
     * exist ({@code not_found}); unsupported URLs and requests that fail to
     * fetch are not charged. None
     * of the page-scraping options apply.
     *
     * <p>The URL is sent exactly as given and is never checked against a
     * list of sites: more sites and page types are added on the server. An
     * unsupported URL or page type throws
     * {@link ai.webscraping.exception.BadRequestException}: a 400 that is not
     * charged. Its message lists what is supported.
     *
     * @throws IllegalArgumentException if {@code opts.url} is null or blank,
     *     or an extra param key is blank, is {@code api_key} or {@code url}
     *     (any case), or names a typed option ({@code country},
     *     {@code transcript}, {@code transcript_language}), whether or not
     *     that option is set
     */
    public DataResult data(DataOptions opts) {
        require(opts, "opts");
        require(opts.getUrl(), "opts.url");
        if (opts.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("opts.url must not be blank");
        }
        QueryEncoder q = new QueryEncoder();
        q.set("url", opts.getUrl());
        if (notEmpty(opts.getCountry())) {
            q.set("country", opts.getCountry());
        }
        if (opts.getTranscript() != null) {
            q.set("transcript", opts.getTranscript());
        }
        if (notEmpty(opts.getTranscriptLanguage())) {
            q.set("transcript_language", opts.getTranscriptLanguage());
        }
        for (Map.Entry<String, Object> e : opts.getParams().entrySet()) {
            String key = e.getKey();
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("opts.params keys must not be blank");
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if ("api_key".equals(lower)) {
                throw new IllegalArgumentException("opts.params must not contain " + key
                    + "; the API key comes from Config");
            }
            if ("url".equals(lower)) {
                throw new IllegalArgumentException("opts.params must not contain " + key + "; use DataOptions.url");
            }
            String typed = DATA_TYPED_PARAMS.get(key);
            if (typed != null) {
                throw new IllegalArgumentException("opts.params must not contain " + key + "; use DataOptions." + typed);
            }
            q.set(key, e.getValue());
        }
        String body = request("/data", q).getBody();
        return Json.read(body, DataResult.class);
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

    private static void requireFinite(Double value, String name) {
        if (value != null && (value.isNaN() || value.isInfinite())) {
            throw new IllegalArgumentException(name + " must be a finite number, got " + value);
        }
    }

    /**
     * Plain decimal with at most 7 fractional digits and no trailing zeros
     * ({@code 1e-7} becomes {@code 0.0000001}, {@code 48.0} becomes {@code 48}):
     * the server accepts only {@code -?\d+(\.\d+)?}, which
     * {@code Double.toString} breaks for small and large values.
     */
    static String formatCoordinate(double value) {
        BigDecimal d = BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
        if (d.signum() == 0) {
            return "0";
        }
        return d.stripTrailingZeros().toPlainString();
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
