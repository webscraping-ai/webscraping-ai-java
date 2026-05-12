/*
 * Hand-run smoke test against the live WebScraping.AI API.
 * Not part of `./gradlew test` — costs ~17 credits per full sweep.
 *
 * Usage:
 *   WEBSCRAPING_AI_API_KEY=... ./gradlew smoke
 */
package ai.webscraping.smoke;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import ai.webscraping.Client;
import ai.webscraping.Config;
import ai.webscraping.exception.ApiException;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;

public final class Smoke {

    private static final String TARGET = "https://example.com";

    private Smoke() {}

    public static void main(String[] args) {
        String apiKey = System.getenv(Config.API_KEY_ENV);
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

        failures += run("html", () -> client.html(HtmlOptions.builder().url(TARGET).build()));

        failures += run("text", () -> client.text(TextOptions.builder().url(TARGET).build()));

        failures += run("selected", () ->
            client.selected(SelectedOptions.builder().url(TARGET).selector("h1").build()));

        failures += run("selected_multiple", () -> {
            SelectedMultipleResult out = client.selectedMultiple(SelectedMultipleOptions.builder()
                .url(TARGET)
                .selectors("h1", "p")
                .build());
            return out.getResults().toString();
        });

        failures += run("question", () ->
            client.question(QuestionOptions.builder()
                .url(TARGET)
                .question("What is this page about? Answer in one sentence.")
                .build()));

        failures += run("fields", () -> {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("title", "Page title");
            fields.put("description", "Short description");
            FieldsResult out = client.fields(FieldsOptions.builder()
                .url(TARGET)
                .fields(fields)
                .build());
            return out.getResult().toString();
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
            System.out.printf("  ok   %-20s %s%n", name, preview);
            return 0;
        } catch (ApiException e) {
            System.out.printf("  FAIL %-20s ApiException(HTTP %d): %s%n",
                name, e.getHttpStatus(), e.getMessage());
            return 1;
        } catch (Exception e) {
            System.out.printf("  FAIL %-20s %s: %s%n", name, e.getClass().getSimpleName(), e.getMessage());
            return 1;
        }
    }
}
