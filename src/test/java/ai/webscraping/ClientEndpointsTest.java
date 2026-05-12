package ai.webscraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;

class ClientEndpointsTest {

    private WireMockServer server;
    private Client client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
        client = new Client(Config.builder()
            .apiKey("test-key")
            .baseUrl("http://localhost:" + server.port())
            .requestTimeout(Duration.ofSeconds(10))
            .build());
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private String lastQueryString() {
        List<LoggedRequest> requests = server.getAllServeEvents().stream()
            .map(e -> e.getRequest())
            .collect(java.util.stream.Collectors.toList());
        if (requests.isEmpty()) {
            return "";
        }
        LoggedRequest req = requests.get(requests.size() - 1);
        java.net.URI uri = java.net.URI.create(req.getAbsoluteUrl());
        return uri.getRawQuery();
    }

    @Test
    void htmlGetsHtmlEndpointWithApiKey() {
        stubFor(get(urlPathEqualTo("/html"))
            .willReturn(aResponse().withStatus(200).withBody("<html>hi</html>")));

        String html = client.html(HtmlOptions.builder().url("https://example.com").build());

        assertThat(html).isEqualTo("<html>hi</html>");
        verify(getRequestedFor(urlPathEqualTo("/html"))
            .withQueryParam("api_key", equalTo("test-key"))
            .withQueryParam("url", equalTo("https://example.com")));
        assertThat(lastQueryString()).startsWith("api_key=test-key&url=");
    }

    @Test
    void textGetsTextEndpointWithTextFormatAndReturnLinks() {
        stubFor(get(urlPathEqualTo("/text"))
            .willReturn(aResponse().withStatus(200).withBody("# Example")));

        String text = client.text(TextOptions.builder()
            .url("https://example.com")
            .textFormat("markdown")
            .returnLinks(true)
            .build());

        assertThat(text).isEqualTo("# Example");
        verify(getRequestedFor(urlPathEqualTo("/text"))
            .withQueryParam("text_format", equalTo("markdown"))
            .withQueryParam("return_links", equalTo("true")));
    }

    @Test
    void selectedGetsSelectedEndpointWithSelector() {
        stubFor(get(urlPathEqualTo("/selected"))
            .willReturn(aResponse().withStatus(200).withBody("<h1>Example Domain</h1>")));

        String out = client.selected(SelectedOptions.builder()
            .url("https://example.com")
            .selector("h1")
            .build());

        assertThat(out).isEqualTo("<h1>Example Domain</h1>");
        verify(getRequestedFor(urlPathEqualTo("/selected"))
            .withQueryParam("selector", equalTo("h1")));
    }

    @Test
    void selectedMultipleParsesArrayOfArrays() {
        stubFor(get(urlPathEqualTo("/selected-multiple"))
            .willReturn(aResponse().withStatus(200)
                .withBody("[[\"Example Domain\",\"This domain is for…\"]]")));

        SelectedMultipleResult out = client.selectedMultiple(SelectedMultipleOptions.builder()
            .url("https://example.com")
            .selectors("h1", "p")
            .build());

        assertThat(out.getResults()).hasSize(1);
        assertThat(out.getResults().get(0)).containsExactly("Example Domain", "This domain is for…");

        // Selectors must be encoded as form-explode WITHOUT brackets.
        String query = lastQueryString();
        assertThat(query).contains("selectors=h1");
        assertThat(query).contains("selectors=p");
        assertThat(query).doesNotContain("selectors%5B%5D");
    }

    @Test
    void questionUnwrapsJsonQuotedAnswerByDefault() {
        stubFor(get(urlPathEqualTo("/ai/question"))
            .willReturn(aResponse().withStatus(200).withBody("\"This page is an example.\"")));

        String answer = client.question(QuestionOptions.builder()
            .url("https://example.com")
            .question("what?")
            .build());

        assertThat(answer).isEqualTo("This page is an example.");
        verify(getRequestedFor(urlPathEqualTo("/ai/question"))
            .withQueryParam("question", equalTo("what?")));
    }

    @Test
    void questionLeavesObjectFormatBodyUnchanged() {
        stubFor(get(urlPathEqualTo("/ai/question"))
            .willReturn(aResponse().withStatus(200).withBody("{\"answer\":\"yes\"}")));

        String answer = client.question(QuestionOptions.builder()
            .url("https://example.com")
            .question("ok?")
            .format("json")
            .build());

        assertThat(answer).isEqualTo("{\"answer\":\"yes\"}");
    }

    @Test
    void fieldsParsesResultWrapper() {
        stubFor(get(urlPathEqualTo("/ai/fields"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"result\":{\"title\":\"Example Domain\",\"price\":\"$10\"}}")));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Page title");
        fields.put("price", "Current price");
        FieldsResult out = client.fields(FieldsOptions.builder()
            .url("https://example.com")
            .fields(fields)
            .build());

        assertThat(out.getResult()).containsEntry("title", "Example Domain").containsEntry("price", "$10");

        // Fields must be encoded as deepObject (fields[title]=..., fields[price]=...).
        String query = lastQueryString();
        assertThat(query).contains("fields%5Btitle%5D=Page%20title");
        assertThat(query).contains("fields%5Bprice%5D=Current%20price");
    }

    @Test
    void accountReturnsTypedAccountInfo() {
        stubFor(get(urlPathEqualTo("/account"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"email\":\"u@example.com\",\"remaining_api_calls\":42,\"resumes_at\":1762000000}")));

        AccountInfo info = client.account();
        assertThat(info.getEmail()).isEqualTo("u@example.com");
        assertThat(info.getRemainingApiCalls()).isEqualTo(42L);
        assertThat(info.getResumesAt()).isEqualTo(1762000000L);
    }

    @Test
    void commonOptionsEncodeAsExpected() {
        stubFor(get(urlPathEqualTo("/html")).willReturn(aResponse().withStatus(200).withBody("ok")));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Cookie", "id=1");
        headers.put("Accept", "text/html");

        client.html(HtmlOptions.builder()
            .url("https://example.com")
            .js(true)
            .jsTimeout(8)
            .country("us")
            .errorOn404(false)
            .headers(headers)
            .build());

        String q = lastQueryString();
        assertThat(q).contains("js=true");
        assertThat(q).contains("js_timeout=8");
        assertThat(q).contains("country=us");
        assertThat(q).contains("error_on_404=false");
        assertThat(q).contains("headers%5BAccept%5D=text%2Fhtml");
        assertThat(q).contains("headers%5BCookie%5D=id%3D1");
    }

    @Test
    void userAgentHeaderIncludesVersion() {
        stubFor(get(urlPathEqualTo("/html")).willReturn(aResponse().withStatus(200).withBody("ok")));

        client.html(HtmlOptions.builder().url("https://example.com").build());

        verify(getRequestedFor(urlPathEqualTo("/html"))
            .withHeader("User-Agent", equalTo("webscraping-ai-java/" + Version.VERSION)));
    }

    @Test
    void selectedMultipleAcceptsListSelectors() {
        stubFor(get(urlPathEqualTo("/selected-multiple"))
            .willReturn(aResponse().withStatus(200).withBody("[[]]")));

        client.selectedMultiple(SelectedMultipleOptions.builder()
            .url("https://example.com")
            .selectors(Arrays.asList("h1", ".x"))
            .build());

        String q = lastQueryString();
        assertThat(q).contains("selectors=h1");
        assertThat(q).contains("selectors=.x");
    }
}
