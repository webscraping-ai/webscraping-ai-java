/*
 * Hand-run smoke test against the live WebScraping.AI API.
 * Not part of `./gradlew test` — costs ~31 credits per full sweep: page
 * tools run with js=false and the datacenter proxy (html/text/selected/
 * selected_multiple 4 x 1, question/fields 2 x 6) plus 15 for the SERP search.
 *
 * Each step asserts on the result shape, not just the absence of an
 * exception; any Throwable is reported as a FAIL line (with the API key
 * redacted) and the sweep continues. Exits non-zero if any step failed.
 *
 * Usage:
 *   WEBSCRAPING_AI_API_KEY=... ./gradlew smoke
 */
package ai.webscraping.smoke;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import ai.webscraping.Client;
import ai.webscraping.Config;
import ai.webscraping.exception.ApiException;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.SerpOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;
import ai.webscraping.result.SerpResult;

public final class Smoke {

    private static final String TARGET = "https://example.com";
    private static final String PROXY = "datacenter";
    private static final String SERP_QUERY = "coffee machines";
    private static final Pattern API_KEY_PARAM = Pattern.compile("api_key=[^&\\s\"']*");

    private static String apiKey;

    private Smoke() {}

    public static void main(String[] args) {
        apiKey = System.getenv(Config.API_KEY_ENV);
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println(Config.API_KEY_ENV + " is required");
            System.exit(2);
        }

        Client client = new Client(Config.builder()
            .apiKey(apiKey)
            .requestTimeout(Duration.ofSeconds(90))
            .build());

        int failures = 0;

        failures += run("account", () -> {
            AccountInfo info = client.account();
            return String.format(Locale.ROOT, "email=%s remaining=%d", info.getEmail(), info.getRemainingApiCalls());
        });

        failures += run("html", () -> nonEmpty(client.html(HtmlOptions.builder()
            .url(TARGET).js(false).proxy(PROXY).build())));

        failures += run("text", () -> nonEmpty(client.text(TextOptions.builder()
            .url(TARGET).js(false).proxy(PROXY).build())));

        failures += run("selected", () -> nonEmpty(client.selected(SelectedOptions.builder()
            .url(TARGET).js(false).proxy(PROXY).selector("h1").build())));

        failures += run("selected_multiple", () -> {
            SelectedMultipleResult out = client.selectedMultiple(SelectedMultipleOptions.builder()
                .url(TARGET)
                .js(false)
                .proxy(PROXY)
                .selectors("h1", "p")
                .build());
            // The API answers 200 [[]] when selectors are mis-encoded.
            List<List<String>> results = out.getResults();
            if (results == null || results.stream().allMatch(inner -> inner == null || inner.isEmpty())) {
                throw new IllegalStateException("no selector matched anything: " + results);
            }
            return results.toString();
        });

        failures += run("question", () -> nonEmpty(client.question(QuestionOptions.builder()
            .url(TARGET)
            .js(false)
            .proxy(PROXY)
            .question("What is this page about? Answer in one sentence.")
            .build())));

        failures += run("fields", () -> {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("title", "Page title");
            fields.put("description", "Short description");
            FieldsResult out = client.fields(FieldsOptions.builder()
                .url(TARGET)
                .js(false)
                .proxy(PROXY)
                .fields(fields)
                .build());
            if (out == null || out.getResult() == null) {
                throw new IllegalStateException("response has no result");
            }
            return out.getResult().toString();
        });

        failures += run("serp", () -> {
            SerpResult out = client.serp(SerpOptions.builder().q(SERP_QUERY).build());
            if (out.getOrganicResults() == null || out.getOrganicResults().isEmpty()) {
                throw new IllegalStateException("no organic_results");
            }
            String echoed = out.getSearchParameters() == null ? null : out.getSearchParameters().getQ();
            if (!SERP_QUERY.equals(echoed)) {
                throw new IllegalStateException("search_parameters.q = " + echoed + ", want " + SERP_QUERY);
            }
            String top = out.getOrganicResults().get(0).getLink();
            return String.format(Locale.ROOT, "state=%s results=%d top=%s",
                out.getSearchInformation().getOrganicResultsState(), out.getOrganicResults().size(), top);
        });

        if (failures > 0) {
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface Step {
        String run() throws Exception;
    }

    private static int run(String name, Step step) {
        try {
            String preview = step.run();
            if (preview != null && preview.length() > 120) {
                preview = preview.substring(0, 120);
            }
            System.out.printf("  ok   %-20s %s%n", name, redact(preview));
            return 0;
        } catch (ApiException e) {
            System.out.printf("  FAIL %-20s ApiException(HTTP %d): %s%n",
                name, e.getHttpStatus(), redact(e.getMessage()));
            return 1;
        } catch (Throwable e) {
            System.out.printf("  FAIL %-20s %s: %s%n", name, e.getClass().getSimpleName(), redact(e.getMessage()));
            return 1;
        }
    }

    private static String nonEmpty(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalStateException("empty result");
        }
        return s;
    }

    /** Scrubs the API key (and any api_key=... pattern) from printed output. */
    private static String redact(String s) {
        if (s == null) {
            return null;
        }
        String out = API_KEY_PARAM.matcher(s).replaceAll("api_key=REDACTED");
        return apiKey == null || apiKey.isEmpty() ? out : out.replace(apiKey, "REDACTED");
    }
}
