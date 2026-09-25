# Changelog

All notable changes to `ai.webscraping:webscraping-ai` are documented in
this file.

## 4.1.0 — 2026-09-25

### Added

- `Client.serp(SerpOptions)` for the new `GET /serp` endpoint: parsed Google search results for a query. `SerpOptions` builder takes `q` (required), `engine`, `gl`, `hl`, `page`; it does not extend `CommonOptions` since the page-scraping options don't apply. Returns a typed `SerpResult` (`getSearchParameters()`, `getSearchInformation()`, `getOrganicResults()`, `getRelatedSearches()`, `getPagination()`); optional response fields return `null` when absent. Flat 15 credits per search; failed searches are not charged.
- `./gradlew smoke` now exercises `serp`.
- `serp` throws `IllegalArgumentException` for a blank (whitespace-only) `q` and a `page` below 1 before sending a request; the server would otherwise coerce an invalid page to 1 and still charge. `q` is sent untrimmed. The server caps `page` at 100.
- `SerpResult.OrganicResult.getPosition()` returns `Integer` (`null` when absent) like the other numeric fields, instead of a primitive `int` that turned a missing position into 0.
- `./gradlew smoke` asserts on results (non-empty page output, at least one non-empty `selectedMultiple` match, `fields` `result` present, `serp` organic results and echoed query), runs page tools with `js=false` and the datacenter proxy (~31 credits per sweep), reports any `Throwable` as a FAIL line, and redacts the API key from output.

### Fixed

- An invalid base URL no longer leaks the API key. `JdkHttpTransport` called `URI.create` on the full request URL (with `api_key`) outside its redaction block, so a base URL like `http://bad host` threw an `IllegalArgumentException` whose message contained the key. `Config` now rejects a base URL that is not an absolute http(s) URL with a host, before any key is attached, and the transport turns a URI failure into an `ApiConnectionException` with a redacted URL and no cause.
- Transport exception messages and causes are scrubbed: any `api_key=...` in an I/O error message is redacted, and a cause whose chain mentions `api_key` is dropped.

## 4.0.2 — 2026-07-17

### Changed

- Documentation: expanded README — API docs, signup/dashboard links, badges, and links to the other official clients.

## 4.0.1 — 2026-06-21

### Fixed

- Transport errors no longer leak the API key: the query string (which carries `api_key`) is redacted from the URL embedded in `ApiTimeoutException` / `ApiConnectionException` messages.
- `AccountInfo` now matches the live `/account` response — `resetsAt` (`resets_at`) and `remainingConcurrency` (`remaining_concurrency`), replacing the stale `resumesAt` / `concurrencyLimit` / `creditsPerMonth`.
- `selected` and `selectedMultiple` no longer require a selector; omitting it returns whole-page HTML, matching the API.

## 4.0.0 — 2026-05-12

First release of the official Java client.

The version starts at `4.0.0` to keep the version line aligned with the
other hand-authored WebScraping.AI SDKs (Ruby, Python, PHP, JavaScript, Go
— all at 4.0.x). There was no earlier Java client; the major bump is
purely for cross-SDK coherence.

### Highlights

- Single `ai.webscraping.Client` class with seven synchronous methods,
  one per endpoint: `html`, `text`, `selected`, `selectedMultiple`,
  `question`, `fields`, `account`.
- Builder-pattern option objects per endpoint (`HtmlOptions`,
  `TextOptions`, `SelectedOptions`, `SelectedMultipleOptions`,
  `QuestionOptions`, `FieldsOptions`).
- Single runtime dependency: `jackson-databind`. HTTP transport built on
  the JDK's `java.net.http.HttpClient` — zero networking deps.
- Pluggable `Transport` interface for swapping in Apache HttpClient 5,
  OkHttp, etc. while keeping the rest of the SDK identical.
- Typed return values where the response shape is stable
  (`AccountInfo`, `FieldsResult`, `SelectedMultipleResult`) and `String`
  for the HTML/text/selected/question endpoints.
- Unchecked exception hierarchy mirroring the other SDKs:
  `WebScrapingAIException` → `ApiException` and per-status subtypes
  (`BadRequestException`, `PaymentRequiredException`,
  `AuthenticationException`, `RateLimitException`, `ServerException`,
  `GatewayTimeoutException`) plus `ApiTimeoutException` /
  `ApiConnectionException` for transport failures.
- `WEBSCRAPING_AI_API_KEY` is read from the environment as a fallback
  when `Config.Builder#apiKey` is not set.
- Java 11 source/target compatibility; CI matrix tests against JDK 11,
  17, and 21.
