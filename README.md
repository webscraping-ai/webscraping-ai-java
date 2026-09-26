# webscraping-ai-java

[![CI](https://github.com/webscraping-ai/webscraping-ai-java/actions/workflows/ci.yml/badge.svg)](https://github.com/webscraping-ai/webscraping-ai-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/ai.webscraping/webscraping-ai.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/ai.webscraping/webscraping-ai)

Official Java client for the [WebScraping.AI](https://webscraping.ai) API —
web scraping with Chromium JavaScript rendering, rotating
datacenter/residential/stealth proxies, and AI-powered question answering and
structured field extraction on any page. See the
[API documentation](https://webscraping.ai/docs) for the full parameter reference.

## Install

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("ai.webscraping:webscraping-ai:4.2.0")
}
```

**Gradle (Groovy DSL):**

```groovy
dependencies {
    implementation 'ai.webscraping:webscraping-ai:4.2.0'
}
```

**Maven:**

```xml
<dependency>
    <groupId>ai.webscraping</groupId>
    <artifactId>webscraping-ai</artifactId>
    <version>4.2.0</version>
</dependency>
```

Requires **Java 11 or newer**. Single runtime dependency: `jackson-databind`.

## Quick start

[Sign up](https://webscraping.ai/auth/sign_up) to get an API key — a free
trial, no credit card required. Your key lives in the
[dashboard](https://webscraping.ai/dashboard).

```java
import ai.webscraping.Client;
import ai.webscraping.Config;
import ai.webscraping.option.*;
import ai.webscraping.result.*;

Client client = new Client(Config.builder()
    .apiKey("YOUR_API_KEY")
    .build());

// Full HTML
String html = client.html(HtmlOptions.builder()
    .url("https://example.com")
    .js(true)
    .build());

// Visible text
String text = client.text(TextOptions.builder()
    .url("https://example.com")
    .textFormat("json")
    .build());

// CSS-selected HTML
String heading = client.selected(SelectedOptions.builder()
    .url("https://example.com")
    .selector("h1")
    .build());

// Multiple selectors at once
SelectedMultipleResult parts = client.selectedMultiple(SelectedMultipleOptions.builder()
    .url("https://example.com")
    .selectors("h1", "p")
    .build());

// LLM-powered helpers
String answer = client.question(QuestionOptions.builder()
    .url("https://example.com")
    .question("What is this page about?")
    .build());

FieldsResult fields = client.fields(FieldsOptions.builder()
    .url("https://example.com")
    .addField("title",       "Main product title")
    .addField("price",       "Current product price")
    .build());

// Google search results
SerpResult serp = client.serp(SerpOptions.builder()
    .q("coffee machines")
    .build());

// Structured data for a page on a supported site
DataResult video = client.data(DataOptions.builder()
    .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    .build());

// Account quota
AccountInfo info = client.account();
System.out.println(info.getEmail() + " — " + info.getRemainingApiCalls() + " remaining");
```

The API key falls back to the `WEBSCRAPING_AI_API_KEY` environment variable when
`Config.Builder#apiKey` is not set, or you can construct an env-only client
directly:

```java
Client client = new Client();   // reads WEBSCRAPING_AI_API_KEY
```

## Search engine results (SERP)

`serp` calls `GET /serp` and returns parsed Google results as a typed
`SerpResult`. It is query-shaped — pass the search query via `q` instead of a
URL. `SerpOptions` does not extend the page-scraping options (`js`, `proxy`,
`country`, … don't apply). Priced per search (see
[pricing](https://webscraping.ai/docs#serp)); failed searches are not charged.

`serp` throws `IllegalArgumentException` for a null or blank (whitespace-only)
`q` and for a `page` below 1, before sending anything. The server also rejects
an invalid page with a 400 (not billed); checking client-side saves the round
trip. Pages are 1–100: the server rejects a `page` above 100 with a 400.

```java
SerpResult serp = client.serp(SerpOptions.builder()
    .q("coffee machines") // required
    .engine("google")     // optional, default "google" (only engine today)
    .gl("de")             // optional two-letter country, default "us"
    .hl("de")             // optional two-letter language, default "en"
    .page(2)              // optional, 1-based, 10 results per page (server rejects > 100 with a 400)
    .build());

System.out.println(serp.getSearchInformation().getOrganicResultsState()); // "Results for exact spelling"
for (SerpResult.OrganicResult r : serp.getOrganicResults()) {
    // position restarts at 1 on every page; (page - 1) * 10 + position is the absolute rank
    System.out.println(r.getPosition() + " " + r.getTitle() + " " + r.getLink() + " " + r.getDomain());
}
if (serp.getRelatedSearches() != null) {
    serp.getRelatedSearches().forEach(rs -> System.out.println("related: " + rs.getQuery()));
}
Integer next = serp.getPagination().getNext(); // null when there is no further page
```

Optional response fields (`getPosition()`, `getSnippet()`, `getDate()`, `getShowingResultsFor()`,
`getTotalResults()`, `getPagination().getNext()`, `getRelatedSearches()`) return
`null` when the API omits them.

## Structured data (`/data`)

`data` calls `GET /data` and returns structured JSON for a public page on a
supported site: pass the page's normal URL and the server detects the site
(`provider`) and page kind (`type`). Sites today include e.g. YouTube, TikTok,
X/Twitter, LinkedIn, Instagram and Reddit, but **more sites and page types are
added on the server**, so the client never checks the URL itself. An
unsupported URL or page type returns a 400 (`BadRequestException`) that is not
charged. Its message lists what is supported. `DataOptions` does
not extend the page-scraping options (`js`, `proxy`, `headers`, … don't apply).
Priced per site (see [pricing](https://webscraping.ai/docs#data)), including
pages that parse empty or no longer exist; unsupported URLs and requests that
fail to fetch are not charged.

`data` throws `IllegalArgumentException` for a null or blank `url` before
sending anything.

```java
DataResult out = client.data(DataOptions.builder()
    .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // required
    .country("us")                                      // optional
    .transcript(true)                                   // optional, YouTube videos only
    .transcriptLanguage("en")                           // optional
    .build());

System.out.println(out.getRequestParameters().getProvider()); // "youtube"
System.out.println(out.getRequestParameters().getType());     // "video"
System.out.println(out.getParseStatus());                     // "ok", "parse_failed" or "not_found"
if (out.getData() != null) {
    System.out.println(out.getData().path("title").asText());
}
```

- `country`: Two-letter country code of the proxy used to fetch the page, `us`
  by default. The server checks it and rejects unknown ones with a 400.
- `transcript`: YouTube videos only. Also fetch the video's transcript into
  `data.transcript`. It's null when no matching captions are available. If the
  transcript fetch itself fails, the whole request fails with a 500 and is not
  charged.
- `transcriptLanguage`: Caption language to pick, e.g. `en` or `de`. Without
  it, English is preferred, then the first available track. If the video has no
  captions in that language, `data.transcript` is null.

`getProvider()`, `getType()` and `getParseStatus()` are plain strings, not
enums, so values added later come through unchanged. `getData()` is a Jackson
`JsonNode` (snake_case keys) whose shape depends on the provider and type; it
is `null` when the API returns `data: null` (possible with `parse_failed` or
`not_found`, which are still successful, charged requests). Map it onto your
own class with `new ObjectMapper().treeToValue(out.getData(), MyVideo.class)`.

Parameters a future provider needs can be sent without a client release via
`.param(key, value)` (or `.params(map)`); values must be a `String`, `Boolean`
or `Number` and are percent-encoded like everything else. A blank key,
`api_key` or `url` (any case), or a typed option's name (`country`,
`transcript`, `transcript_language`, whether or not that option is set) throws
`IllegalArgumentException`: use the typed setter instead.

```java
client.data(DataOptions.builder()
    .url("https://www.tiktok.com/@nasa")
    .param("some_new_option", "value")
    .build());
```

## Configuration

```java
Config cfg = Config.builder()
    .apiKey("YOUR_API_KEY")
    .baseUrl("https://api.webscraping.ai")          // default
    .requestTimeout(Duration.ofSeconds(60))          // default
    .userAgent("my-app/1.0 (+webscraping-ai-java)") // optional override
    .transport(new JdkHttpTransport(myHttpClient))  // optional injection
    .build();
```

`requestTimeout` is the fallback per-request deadline. Pass a non-positive
`Duration` to disable the implicit timeout entirely (the caller manages it).

To plug in a different HTTP client (Apache HttpClient 5, OkHttp, …), implement
`Transport` and pass it to `Config.Builder#transport`.

## Error handling

Every non-2xx response throws a typed `ApiException` subclass; transport-level
failures throw `ApiTimeoutException` or `ApiConnectionException`. All extend
`WebScrapingAIException` (unchecked).

```java
import ai.webscraping.exception.*;

try {
    client.html(HtmlOptions.builder().url("https://example.com").build());
} catch (AuthenticationException e) {
    // 403 — wrong or missing API key
} catch (RateLimitException e) {
    // 429 — too many concurrent requests
} catch (ApiTimeoutException e) {
    // request did not complete in time
} catch (ApiException e) {
    // any other HTTP response error (400/402/500/504/…)
} catch (WebScrapingAIException e) {
    // catch-all for anything from this SDK
}
```

Full hierarchy:

- `WebScrapingAIException` (base — extends `RuntimeException`)
  - `ApiException` (HTTP response received, non-2xx)
    - `BadRequestException` — HTTP 400
    - `PaymentRequiredException` — HTTP 402
    - `AuthenticationException` — HTTP 403
    - `RateLimitException` — HTTP 429
    - `ServerException` — HTTP 500
    - `GatewayTimeoutException` — HTTP 504
  - `ApiTimeoutException` — no response, deadline elapsed
  - `ApiConnectionException` — no response, transport-level error

`ApiException` exposes the API's documented error envelope via `getStatusCode()`,
`getStatusMessage()`, `getBody()`, and the raw `getResponseBody()`.

## Response shapes

Two endpoints return shapes that differ from the OpenAPI spec — upstream drift
reproduced by every official SDK:

- **`fields`** wraps the extracted fields under `result`:
  `FieldsResult.getResult()` returns `Map<String, String>`.
- **`selectedMultiple`** returns `List<List<String>>` (one outer wrapper
  containing all matches), not the flat list the spec implies.
  `SelectedMultipleResult.getResults()` returns the outer list directly.

## Development

```bash
./gradlew build               # compile, test, lint, package
./gradlew test                # tests only
./gradlew checkstyleMain spotbugsMain
./gradlew javadoc

# Live smoke test (hits production, ~46 credits per sweep):
WEBSCRAPING_AI_API_KEY=... ./gradlew smoke
```

Local Java + Gradle versions are pinned via [mise](https://mise.jdx.dev/)
(`mise install` from the repo root).

## Links

- [WebScraping.AI](https://webscraping.ai) — features, pricing, signup
- [API documentation](https://webscraping.ai/docs)
- [Dashboard](https://webscraping.ai/dashboard) — API key, usage, request builder
- Other official clients: [Python](https://github.com/webscraping-ai/webscraping-ai-python) · [JavaScript](https://github.com/webscraping-ai/webscraping-ai-js) · [Ruby](https://github.com/webscraping-ai/webscraping-ai-ruby) · [PHP](https://github.com/webscraping-ai/webscraping-ai-php) · [Go](https://github.com/webscraping-ai/webscraping-ai-go) · [.NET](https://github.com/webscraping-ai/webscraping-ai-dotnet) · [CLI](https://github.com/webscraping-ai/webscraping-ai-cli) · [MCP server](https://github.com/webscraping-ai/webscraping-ai-mcp-server) · [n8n node](https://github.com/webscraping-ai/webscraping-ai-n8n)
- Support: [support@webscraping.ai](mailto:support@webscraping.ai)

## License

MIT — see [LICENSE](LICENSE).
