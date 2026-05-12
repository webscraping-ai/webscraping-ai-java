# webscraping-ai-java

[![CI](https://github.com/webscraping-ai/webscraping-ai-java/actions/workflows/ci.yml/badge.svg)](https://github.com/webscraping-ai/webscraping-ai-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/ai.webscraping/webscraping-ai.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/ai.webscraping/webscraping-ai)

Official Java client for the [WebScraping.AI](https://webscraping.ai) API.

## Install

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("ai.webscraping:webscraping-ai:4.0.0")
}
```

**Gradle (Groovy DSL):**

```groovy
dependencies {
    implementation 'ai.webscraping:webscraping-ai:4.0.0'
}
```

**Maven:**

```xml
<dependency>
    <groupId>ai.webscraping</groupId>
    <artifactId>webscraping-ai</artifactId>
    <version>4.0.0</version>
</dependency>
```

Requires **Java 11 or newer**. Single runtime dependency: `jackson-databind`.

## Quick start

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

# Live smoke test (hits production, ~17 credits per sweep):
WEBSCRAPING_AI_API_KEY=... ./gradlew smoke
```

Local Java + Gradle versions are pinned via [mise](https://mise.jdx.dev/)
(`mise install` from the repo root).

## License

MIT — see [LICENSE](LICENSE).
