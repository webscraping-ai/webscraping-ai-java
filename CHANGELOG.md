# Changelog

All notable changes to `ai.webscraping:webscraping-ai` are documented in
this file.

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
