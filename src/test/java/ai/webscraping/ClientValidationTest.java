package ai.webscraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
