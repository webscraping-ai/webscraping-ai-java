# Changelog

All notable changes to `ai.webscraping:webscraping-ai` are documented in
this file.

## 4.3.1 — 2026-10-24
### Security

- Bump `jackson-databind` 2.18.2 → 2.18.12 (and with it `jackson-core`), picking up the 2.18-line fixes for the published jackson-databind and jackson-core advisories, among them number-parsing and forward-reference denial-of-service issues in response parsing and several polymorphic-typing validator bypasses.

## 4.3.0 — 2026-10-24
### Added

- `SerpOptions` gains `pages`, `location`, `uule`, `lat`, `lon` and `radius`. Every search runs from one fixed location (`location`, `lat` + `lon` or `uule`, otherwise the capital of the `gl` country), so repeats and later pages are consistent; `gl` defaults to the location's country. `pages` (1–5) fetches consecutive pages as one de-duplicated list, billed per results page returned (see https://webscraping.ai/docs#serp). `location` and `uule` are sent as given; `lat`/`lon` are sent as plain decimals rounded to 7 fractional digits (never exponent notation). `serp` throws `IllegalArgumentException` for a `pages` or `radius` below 1 and a NaN or infinite `lat`/`lon` before sending; upper bounds and which location options may be combined are left to the server (a 400, not billed).
- `SerpResult` gains `SearchParameters.getPages()`, `getLocationRequested()`, `getLocationUsed()`, `getLat()`, `getLon()`, `getRadius()`, `OrganicResult.getPage()` and `Pagination.getPagesReturned()`, all `null` when absent. With `pages` above 1, `position` counts through the combined list. The previous public constructors are kept.
- `Client.serpLocations(SerpLocationsOptions)` (and a no-arg `serpLocations()`) for the new `GET /serp/locations` endpoint: look up names for `location`. `SerpLocationsOptions` takes `q` and `limit` (`limit` below 1 throws `IllegalArgumentException`). Returns `List<SerpLocation>` with `getId()` (string), `getGoogleId()`/`getGoogleParentId()`/`getReach()` (`Long`, parent `null` for countries), `getName()`, `getCanonicalName()`, `getCountryCode()`, `getTargetType()` (an open string) and `getGps()` (`[longitude, latitude]`). Free; no API key needed on the server.
- `./gradlew smoke` adds a two-page `serp` from `Austin, TX` (asserts `location_used` contains Austin and non-empty organic results) and `serpLocations("austin", limit 3)` (asserts the first `canonical_name` contains Austin).

### Changed

- Docs: stop stating credit prices (they're set server-side and change); link to https://webscraping.ai/docs pricing instead.

## 4.2.0 — 2026-09-25
### Added

- `Client.data(DataOptions)` for the new `GET /data` endpoint: structured JSON for a page on a supported site (e.g. YouTube, TikTok, X/Twitter, LinkedIn, Instagram, Reddit), detected from the page's normal URL. `DataOptions` builder takes `url` (required, must not be blank), `country`, `transcript`, `transcriptLanguage`, and `param(key, value)` / `params(map)` for extra scalar (String/Boolean/Number) query parameters sent as-is; it does not extend `CommonOptions`. An extra param with a blank key, `api_key` or `url` (any case), or a typed option's name (`country`, `transcript`, `transcript_language`, whether or not that option is set) throws `IllegalArgumentException`. Returns `DataResult` (`getRequestParameters()` → `DataRequestParameters` with `getUrl()`/`getProvider()`/`getType()`, `getParseStatus()`, and `getData()` as a Jackson `JsonNode`, `null` for `data: null`). Provider, type and parse status are plain strings, not enums. The URL is never validated against a site list: more sites are added server-side. An unsupported URL or page type returns a 400 (`BadRequestException`) that is not charged; its message lists what is supported. Priced per site (see https://webscraping.ai/docs#data).
- `./gradlew smoke` now exercises `data` on a YouTube video (asserts `parse_status` `ok`, provider `youtube`, a non-empty `title`) and on `https://example.com/` (asserts the server's 400 with its `Unsupported URL` message).

## 4.1.0 — 2026-09-25

### Added

- `Client.serp(SerpOptions)` for the new `GET /serp` endpoint: parsed Google search results for a query. `SerpOptions` builder takes `q` (required), `engine`, `gl`, `hl`, `page`; it does not extend `CommonOptions` since the page-scraping options don't apply. Returns a typed `SerpResult` (`getSearchParameters()`, `getSearchInformation()`, `getOrganicResults()`, `getRelatedSearches()`, `getPagination()`); optional response fields return `null` when absent. Priced per search (see https://webscraping.ai/docs#serp); failed searches are not charged.
- `./gradlew smoke` now exercises `serp`.
- `serp` throws `IllegalArgumentException` for a blank (whitespace-only) `q` and a `page` below 1 before sending a request; the server also rejects an invalid page with a 400 (not billed), so checking client-side saves the round trip. `q` is sent untrimmed. Pages are 1–100: the server rejects a `page` above 100 with a 400.
- `SerpResult.OrganicResult.getPosition()` returns `Integer` (`null` when absent) like the other numeric fields, instead of a primitive `int` that turned a missing position into 0.
- `./gradlew smoke` asserts on results (non-empty page output, at least one non-empty `selectedMultiple` match, `fields` `result` present, `serp` organic results and echoed query), runs page tools with `js=false` and the datacenter proxy to keep the sweep cheap, reports any `Throwable` as a FAIL line, and redacts the API key from output.

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
