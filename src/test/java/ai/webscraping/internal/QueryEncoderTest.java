package ai.webscraping.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QueryEncoderTest {

    @Test
    void flatScalarsAreFlatKeyValue() {
        QueryEncoder q = new QueryEncoder().set("url", "https://example.com").set("timeout", 5000);
        assertThat(q.encode()).isEqualTo("url=https%3A%2F%2Fexample.com&timeout=5000");
    }

    @Test
    void booleansSerializeAsTrueOrFalse() {
        QueryEncoder q = new QueryEncoder().set("js", Boolean.TRUE).set("error_on_404", Boolean.FALSE);
        assertThat(q.encode()).isEqualTo("js=true&error_on_404=false");
    }

    @Test
    void spacesEncodeAsPercent20() {
        QueryEncoder q = new QueryEncoder().set("question", "what is this");
        assertThat(q.encode()).isEqualTo("question=what%20is%20this");
    }

    @Test
    void unreservedCharsAreNotEscaped() {
        QueryEncoder q = new QueryEncoder().set("k", "abc-_.~XYZ123");
        assertThat(q.encode()).isEqualTo("k=abc-_.~XYZ123");
    }

    @Test
    void deepObjectMapsUseBracketsAndSortByKey() {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        // Insertion order is intentionally out-of-alphabetical to verify sort.
        headers.put("X-Token", "abc");
        headers.put("Accept", "text/html");
        headers.put("Cookie", "id=1");

        QueryEncoder q = new QueryEncoder().set("headers", headers);
        assertThat(q.encode()).isEqualTo(
            "headers%5BAccept%5D=text%2Fhtml&headers%5BCookie%5D=id%3D1&headers%5BX-Token%5D=abc"
        );
    }

    @Test
    void listsRepeatTheSameKeyWithoutBrackets() {
        QueryEncoder q = new QueryEncoder().set("selectors", Arrays.asList("h1", ".price", "p"));
        assertThat(q.encode()).isEqualTo("selectors=h1&selectors=.price&selectors=p");
    }

    @Test
    void nullValueClearsTheKey() {
        QueryEncoder q = new QueryEncoder().set("k", "v").set("k", null);
        assertThat(q.encode()).isEmpty();
    }

    @Test
    void emptyListIsDropped() {
        QueryEncoder q = new QueryEncoder().set("selectors", java.util.Collections.emptyList());
        assertThat(q.encode()).isEmpty();
    }

    @Test
    void emptyMapIsDropped() {
        QueryEncoder q = new QueryEncoder().set("headers", java.util.Collections.emptyMap());
        assertThat(q.encode()).isEmpty();
    }

    @Test
    void nullValueInsideMapIsDropped() {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Cookie", "id=1");
        headers.put("X-Skip", null);
        QueryEncoder q = new QueryEncoder().set("headers", headers);
        assertThat(q.encode()).isEqualTo("headers%5BCookie%5D=id%3D1");
    }

    @Test
    void nullValueInsideListIsDropped() {
        QueryEncoder q = new QueryEncoder().set("selectors", Arrays.asList("h1", null, "p"));
        assertThat(q.encode()).isEqualTo("selectors=h1&selectors=p");
    }

    @Test
    void setReplacesInPlacePreservingOrder() {
        QueryEncoder q = new QueryEncoder()
            .set("a", "1")
            .set("b", "2")
            .set("a", "10");
        assertThat(q.encode()).isEqualTo("a=10&b=2");
    }

    @Test
    void insertionOrderIsPreservedAcrossTypes() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Page title");
        QueryEncoder q = new QueryEncoder()
            .set("url", "https://example.com")
            .set("fields", fields)
            .set("js", Boolean.TRUE);
        assertThat(q.encode()).isEqualTo(
            "url=https%3A%2F%2Fexample.com&fields%5Btitle%5D=Page%20title&js=true"
        );
    }

    @Test
    void unsupportedValueTypeThrows() {
        QueryEncoder q = new QueryEncoder();
        assertThatThrownBy(() -> q.set("k", new Object()).encode())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported value type");
    }

    @Test
    void nestedMapInsideMapThrows() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("a", new LinkedHashMap<>(Map.of("inner", "x")));
        QueryEncoder q = new QueryEncoder().set("headers", nested);
        assertThatThrownBy(q::encode)
            .isInstanceOf(IllegalArgumentException.class);
    }
}
