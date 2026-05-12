package ai.webscraping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
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
    void selectedRequiresUrlAndSelector() {
        assertThatThrownBy(() ->
            client.selected(SelectedOptions.builder().url("https://example.com").build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opts.selector");
    }

    @Test
    void selectedMultipleRequiresAtLeastOneSelector() {
        assertThatThrownBy(() ->
            client.selectedMultiple(SelectedMultipleOptions.builder()
                .url("https://example.com")
                .selectors(Collections.emptyList())
                .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("selector");
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
}
