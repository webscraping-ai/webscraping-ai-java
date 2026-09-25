package ai.webscraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.webscraping.option.DataOptions;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.SerpOptions;
import ai.webscraping.option.TextOptions;

class ClientValidationTest {

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client(Config.builder().apiKey("test-key").build());
    }

    @Test
    void htmlRequiresUrl() {
        assertThatThrownBy(() -> client.html(HtmlOptions.builder().build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.url");
    }

    @Test
    void textRequiresUrl() {
        assertThatThrownBy(() -> client.text(TextOptions.builder().build()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectedRequiresUrl() {
        assertThatThrownBy(() ->
            client.selected(SelectedOptions.builder().selector("h1").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.url");
    }

    @Test
    void selectedMultipleRequiresUrl() {
        assertThatThrownBy(() ->
            client.selectedMultiple(SelectedMultipleOptions.builder()
                .selectors(Collections.singletonList("h1"))
                .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.url");
    }

    @Test
    void questionRequiresUrlAndQuestion() {
        assertThatThrownBy(() ->
            client.question(QuestionOptions.builder().url("https://example.com").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.question");
    }

    @Test
    void fieldsRequiresAtLeastOneField() {
        assertThatThrownBy(() ->
            client.fields(FieldsOptions.builder()
                .url("https://example.com")
                .fields(Collections.emptyMap())
                .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("field");
    }

    @Test
    void serpRequiresQ() {
        assertThatThrownBy(() -> client.serp(SerpOptions.builder().gl("us").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.q");
        assertThatThrownBy(() -> client.serp(SerpOptions.builder().q("").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.q");
    }

    @Test
    void serpRejectsBlankQBeforeAnyRequest() {
        Client counting = countingClient();
        for (String q : new String[] {" ", "\t\n  "}) {
            assertThatThrownBy(() -> counting.serp(SerpOptions.builder().q(q).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opts.q");
        }
        assertThat(requests.get()).isZero();
    }

    @Test
    void serpRejectsPageBelowOneBeforeAnyRequest() {
        Client counting = countingClient();
        for (int page : new int[] {0, -1, Integer.MIN_VALUE}) {
            assertThatThrownBy(() -> counting.serp(SerpOptions.builder().q("coffee").page(page).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opts.page");
        }
        assertThat(requests.get()).isZero();
    }

    private final AtomicInteger requests = new AtomicInteger();

    private Client countingClient() {
        return new Client(Config.builder()
            .apiKey("test-key")
            .transport(req -> {
                requests.incrementAndGet();
                return new Transport.Response(200, "{}");
            })
            .build());
    }

    @Test
    void serpRequiresOpts() {
        assertThatThrownBy(() -> client.serp(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts");
    }

    @Test
    void dataRejectsMissingOrBlankUrlBeforeAnyRequest() {
        Client counting = countingClient();
        assertThatThrownBy(() -> counting.data(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts");
        for (String url : new String[] {null, "", " ", "\t\n  "}) {
            assertThatThrownBy(() -> counting.data(DataOptions.builder().url(url).country("us").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opts.url");
        }
        assertThat(requests.get()).isZero();
    }

    @Test
    void dataRejectsApiKeyOrUrlInExtraParamsBeforeAnyRequest() {
        Client counting = countingClient();
        for (String key : new String[] {"api_key", "url"}) {
            assertThatThrownBy(() -> counting.data(DataOptions.builder()
                .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .param(key, "override")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(key);
        }
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("api_key", "other");
        assertThatThrownBy(() -> counting.data(DataOptions.builder().url("https://x.com/nasa").params(map).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("api_key");
        assertThat(requests.get()).isZero();
    }

    @Test
    void dataRejectsCaseVariantsOfApiKeyAndUrlInExtraParams() {
        Client counting = countingClient();
        for (String key : new String[] {"API_KEY", "Api_Key", "URL", "Url"}) {
            assertThatThrownBy(() -> counting.data(DataOptions.builder()
                .url("https://x.com/nasa").param(key, "override").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(key);
        }
        assertThat(requests.get()).isZero();
    }

    @Test
    void dataRejectsExtraParamNamedLikeATypedOptionWhetherOrNotItIsSet() {
        Client counting = countingClient();
        String[][] cases = {
            {"country", "country"}, {"transcript", "transcript"}, {"transcript_language", "transcriptLanguage"},
        };
        for (String[] c : cases) {
            assertThatThrownBy(() -> counting.data(DataOptions.builder()
                .url("https://x.com/nasa").param(c[0], "x").build()))
                .as("unset " + c[0])
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("use DataOptions." + c[1]);
        }
        assertThatThrownBy(() -> counting.data(DataOptions.builder()
            .url("https://x.com/nasa").country("us").param("country", "de").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("use DataOptions.country");
        assertThat(requests.get()).isZero();
    }

    @Test
    void dataParamRejectsInvalidKeysAndValues() {
        assertThatThrownBy(() -> DataOptions.builder().param(null, "x"))
            .isInstanceOf(IllegalArgumentException.class);
        for (String blank : new String[] {"", " ", "\t\n"}) {
            assertThatThrownBy(() -> DataOptions.builder().param(blank, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }
        assertThatThrownBy(() -> DataOptions.builder().param("k", null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DataOptions.builder().param("k", java.util.List.of("a")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
