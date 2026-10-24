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

import ai.webscraping.option.DataOptions;
import ai.webscraping.option.FieldsOptions;
import ai.webscraping.option.HtmlOptions;
import ai.webscraping.option.QuestionOptions;
import ai.webscraping.option.SelectedMultipleOptions;
import ai.webscraping.option.SelectedOptions;
import ai.webscraping.option.SerpLocationsOptions;
import ai.webscraping.option.SerpOptions;
import ai.webscraping.option.TextOptions;
import ai.webscraping.result.AccountInfo;
import ai.webscraping.result.DataResult;
import ai.webscraping.result.FieldsResult;
import ai.webscraping.result.SelectedMultipleResult;
import ai.webscraping.result.SerpLocation;
import ai.webscraping.result.SerpResult;

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
        // getAllServeEvents() is newest-first.
        LoggedRequest req = requests.get(0);
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
    void selectedWithoutSelectorOmitsParam() {
        stubFor(get(urlPathEqualTo("/selected"))
            .willReturn(aResponse().withStatus(200).withBody("<html></html>")));

        String out = client.selected(SelectedOptions.builder()
            .url("https://example.com")
            .build());

        assertThat(out).isEqualTo("<html></html>");
        assertThat(lastQueryString()).doesNotContain("selector=");
    }

    @Test
    void selectedMultipleWithoutSelectorsOmitsParam() {
        stubFor(get(urlPathEqualTo("/selected-multiple"))
            .willReturn(aResponse().withStatus(200).withBody("[[]]")));

        SelectedMultipleResult out = client.selectedMultiple(SelectedMultipleOptions.builder()
            .url("https://example.com")
            .build());

        assertThat(out.getResults()).hasSize(1);
        assertThat(lastQueryString()).doesNotContain("selectors=");
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
                .withBody("{\"email\":\"u@example.com\",\"remaining_api_calls\":42,"
                    + "\"resets_at\":1762000000,\"remaining_concurrency\":5}")));

        AccountInfo info = client.account();
        assertThat(info.getEmail()).isEqualTo("u@example.com");
        assertThat(info.getRemainingApiCalls()).isEqualTo(42L);
        assertThat(info.getResetsAt()).isEqualTo(1762000000L);
        assertThat(info.getRemainingConcurrency()).isEqualTo(5);
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

    @Test
    void serpReturnsTypedSerpResult() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"search_parameters\":{\"engine\":\"google\",\"q\":\"coffee machines\","
                    + "\"gl\":\"de\",\"hl\":\"de\",\"page\":2},"
                    + "\"search_information\":{\"query_displayed\":\"coffee machines\","
                    + "\"organic_results_state\":\"Results for exact spelling\",\"total_results\":160000000},"
                    + "\"organic_results\":["
                    + "{\"position\":1,\"title\":\"Best Coffee Machines\",\"link\":\"https://www.example.com/best\","
                    + "\"domain\":\"example.com\",\"displayed_link\":\"www.example.com › Reviews\","
                    + "\"snippet\":\"We tested 20 machines\",\"date\":\"Apr 13, 2026\"},"
                    + "{\"position\":2,\"title\":\"Other\",\"link\":\"https://other.test/\","
                    + "\"domain\":\"other.test\",\"displayed_link\":\"other.test\"}],"
                    + "\"related_searches\":[{\"query\":\"best espresso machine\"}],"
                    + "\"pagination\":{\"current\":2,\"next\":3}}")));

        SerpResult out = client.serp(SerpOptions.builder()
            .q("coffee machines")
            .engine("google")
            .gl("de")
            .hl("de")
            .page(2)
            .build());

        assertThat(lastQueryString())
            .isEqualTo("api_key=test-key&q=coffee%20machines&engine=google&gl=de&hl=de&page=2");

        assertThat(out.getSearchParameters().getQ()).isEqualTo("coffee machines");
        assertThat(out.getSearchParameters().getPage()).isEqualTo(2);
        assertThat(out.getSearchInformation().getOrganicResultsState()).isEqualTo("Results for exact spelling");
        assertThat(out.getSearchInformation().getShowingResultsFor()).isNull();
        assertThat(out.getSearchInformation().getTotalResults()).isEqualTo(160000000L);

        assertThat(out.getOrganicResults()).hasSize(2);
        SerpResult.OrganicResult first = out.getOrganicResults().get(0);
        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(first.getDomain()).isEqualTo("example.com");
        assertThat(first.getDisplayedLink()).isEqualTo("www.example.com › Reviews");
        assertThat(first.getSnippet()).isEqualTo("We tested 20 machines");
        assertThat(first.getDate()).isEqualTo("Apr 13, 2026");
        SerpResult.OrganicResult second = out.getOrganicResults().get(1);
        assertThat(second.getSnippet()).isNull();
        assertThat(second.getDate()).isNull();

        assertThat(out.getRelatedSearches()).hasSize(1);
        assertThat(out.getRelatedSearches().get(0).getQuery()).isEqualTo("best espresso machine");
        assertThat(out.getPagination().getCurrent()).isEqualTo(2);
        assertThat(out.getPagination().getNext()).isEqualTo(3);
    }

    @Test
    void serpSendsQueryUntrimmedAndKeepsAbsentPositionNull() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"search_parameters\":{\"q\":\" coffee \"},\"search_information\":{},"
                    + "\"organic_results\":[{\"title\":\"No position\",\"link\":\"https://x.test/\"}],"
                    + "\"pagination\":{\"current\":1}}")));

        SerpResult out = client.serp(SerpOptions.builder().q(" coffee ").page(1).build());

        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=%20coffee%20&page=1");
        assertThat(out.getOrganicResults().get(0).getPosition()).isNull();
    }

    @Test
    void serpOmitsUnsetOptionalParamsAndHandlesAbsentFields() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"search_parameters\":{\"engine\":\"google\",\"q\":\"asdf\",\"gl\":\"us\","
                    + "\"hl\":\"en\",\"page\":1},\"search_information\":{\"query_displayed\":\"asdf\","
                    + "\"organic_results_state\":\"Fully empty\"},\"organic_results\":[],"
                    + "\"pagination\":{\"current\":1}}")));

        SerpResult out = client.serp(SerpOptions.builder().q("asdf").build());

        // Optional params are dropped so the API applies its defaults; no scraping params leak in.
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=asdf");
        assertThat(out.getSearchInformation().getOrganicResultsState()).isEqualTo("Fully empty");
        assertThat(out.getSearchInformation().getTotalResults()).isNull();
        assertThat(out.getOrganicResults()).isEmpty();
        assertThat(out.getRelatedSearches()).isNull();
        assertThat(out.getPagination().getNext()).isNull();
    }

    @Test
    void serpSendsLocationAndPagesAndParsesMultiPageFields() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"search_parameters\":{\"engine\":\"google\",\"q\":\"coffee machines\","
                    + "\"gl\":\"us\",\"hl\":\"en\",\"page\":1,\"pages\":2,"
                    + "\"location_requested\":\"Austin, TX\",\"location_used\":\"Austin,Texas,United States\","
                    + "\"lat\":30.2672,\"lon\":-97.7431,\"radius\":500},"
                    + "\"search_information\":{\"organic_results_state\":\"Results for exact spelling\"},"
                    + "\"organic_results\":["
                    + "{\"position\":1,\"title\":\"A\",\"link\":\"https://a.test/\",\"page\":1},"
                    + "{\"position\":11,\"title\":\"B\",\"link\":\"https://b.test/\",\"page\":2}],"
                    + "\"pagination\":{\"current\":2,\"next\":3,\"pages_returned\":2}}")));

        SerpResult out = client.serp(SerpOptions.builder()
            .q("coffee machines")
            .location("Austin, TX")
            .pages(2)
            .radius(500)
            .build());

        assertThat(lastQueryString())
            .isEqualTo("api_key=test-key&q=coffee%20machines&pages=2&location=Austin%2C%20TX&radius=500");

        SerpResult.SearchParameters sp = out.getSearchParameters();
        assertThat(sp.getPages()).isEqualTo(2);
        assertThat(sp.getLocationRequested()).isEqualTo("Austin, TX");
        assertThat(sp.getLocationUsed()).isEqualTo("Austin,Texas,United States");
        assertThat(sp.getRadius()).isEqualTo(500);
        assertThat(sp.getLat()).isEqualTo(30.2672);
        assertThat(sp.getLon()).isEqualTo(-97.7431);
        assertThat(out.getOrganicResults().get(0).getPage()).isEqualTo(1);
        assertThat(out.getOrganicResults().get(1).getPage()).isEqualTo(2);
        assertThat(out.getOrganicResults().get(1).getPosition()).isEqualTo(11);
        assertThat(out.getPagination().getPagesReturned()).isEqualTo(2);
    }

    @Test
    void serpSendsUuleAndLocationUntrimmedAndLeavesSinglePageFieldsNull() {
        stubFor(get(urlPathEqualTo("/serp"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"search_parameters\":{\"q\":\"x\",\"lat\":30.2672,\"lon\":-97.7431},"
                    + "\"organic_results\":[{\"position\":1}],\"pagination\":{\"current\":1}}")));

        client.serp(SerpOptions.builder().q("x").uule("w+CAIQICIGQXVzdGlu").build());
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=x&uule=w%2BCAIQICIGQXVzdGlu");

        client.serp(SerpOptions.builder().q("x").location(" \u041c\u043e\u0441\u043a\u0432\u0430 ").build());
        verify(getRequestedFor(urlPathEqualTo("/serp"))
            .withQueryParam("location", equalTo(" \u041c\u043e\u0441\u043a\u0432\u0430 ")));

        SerpResult out = client.serp(SerpOptions.builder().q("x").lat(30.2672).lon(-97.7431).build());
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=x&lat=30.2672&lon=-97.7431");
        assertThat(out.getSearchParameters().getLat()).isEqualTo(30.2672);
        assertThat(out.getSearchParameters().getLon()).isEqualTo(-97.7431);
        assertThat(out.getSearchParameters().getPages()).isNull();
        assertThat(out.getSearchParameters().getLocationUsed()).isNull();
        assertThat(out.getOrganicResults().get(0).getPage()).isNull();
        assertThat(out.getPagination().getPagesReturned()).isNull();
    }

    @Test
    void serpSendsLatLonAsPlainDecimals() {
        stubFor(get(urlPathEqualTo("/serp")).willReturn(aResponse().withStatus(200).withBody("{}")));

        // Double.toString would give 1.0E-7, 1.0E-5, 48.0, -0.0, 1.0E20 - all rejected by the server.
        double[][] cases = {
            {1e-7, 1e-5}, {1e-9, -1e-9}, {48, -97}, {-0.0, 0.0}, {0.00000005, -0.00000005},
            {30.26721234, -97.74319999}, {89.9999999, -180}, {1e20, 12.5},
        };
        String[][] want = {
            {"0.0000001", "0.00001"}, {"0", "0"}, {"48", "-97"}, {"0", "0"}, {"0.0000001", "-0.0000001"},
            {"30.2672123", "-97.7432"}, {"89.9999999", "-180"}, {"100000000000000000000", "12.5"},
        };
        java.util.Locale saved = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY); // decimal comma must not leak in
            for (int i = 0; i < cases.length; i++) {
                client.serp(SerpOptions.builder().q("x").lat(cases[i][0]).lon(cases[i][1]).build());
                assertThat(lastQueryString())
                    .as("lat=%s lon=%s", cases[i][0], cases[i][1])
                    .isEqualTo("api_key=test-key&q=x&lat=" + want[i][0] + "&lon=" + want[i][1]);
            }
        } finally {
            java.util.Locale.setDefault(saved);
        }
    }

    @Test
    void serpLocationsEncodesQueryAndReturnsTypedLocations() {
        stubFor(get(urlPathEqualTo("/serp/locations"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"1026201\",\"google_id\":1026201,\"google_parent_id\":21176,"
                    + "\"name\":\"Austin\",\"canonical_name\":\"Austin,Texas,United States\","
                    + "\"country_code\":\"US\",\"target_type\":\"City\",\"reach\":974447,"
                    + "\"gps\":[-97.7431,30.2672]},"
                    + "{\"id\":\"2840\",\"google_id\":2840,\"name\":\"United States\","
                    + "\"canonical_name\":\"United States\",\"country_code\":\"US\","
                    + "\"target_type\":\"Neighborhood Cluster\",\"reach\":9000000000,\"future\":1}]")));

        List<SerpLocation> out = client.serpLocations(SerpLocationsOptions.builder().q("san antonio").limit(3).build());

        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=san%20antonio&limit=3");
        assertThat(out).hasSize(2);
        SerpLocation austin = out.get(0);
        assertThat(austin.getId()).isEqualTo("1026201");
        assertThat(austin.getGoogleId()).isEqualTo(1026201L);
        assertThat(austin.getGoogleParentId()).isEqualTo(21176L);
        assertThat(austin.getName()).isEqualTo("Austin");
        assertThat(austin.getCanonicalName()).isEqualTo("Austin,Texas,United States");
        assertThat(austin.getCountryCode()).isEqualTo("US");
        assertThat(austin.getTargetType()).isEqualTo("City");
        assertThat(austin.getReach()).isEqualTo(974447L);
        assertThat(austin.getGps()).containsExactly(-97.7431, 30.2672);
        SerpLocation country = out.get(1);
        assertThat(country.getGoogleParentId()).isNull();
        assertThat(country.getGps()).isNull();
        // Open string, not an enum; reach is 64-bit.
        assertThat(country.getTargetType()).isEqualTo("Neighborhood Cluster");
        assertThat(country.getReach()).isEqualTo(9000000000L);
    }

    @Test
    void serpLocationsOmitsUnsetParams() {
        stubFor(get(urlPathEqualTo("/serp/locations")).willReturn(aResponse().withStatus(200).withBody("[]")));

        assertThat(client.serpLocations()).isEmpty();
        assertThat(lastQueryString()).isEqualTo("api_key=test-key");

        client.serpLocations(SerpLocationsOptions.builder().q("austin").build());
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&q=austin");
    }

    @Test
    void dataEncodesUrlCountryTranscriptAndExtraParams() {
        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"request_parameters\":{\"url\":\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\","
                    + "\"provider\":\"youtube\",\"type\":\"video\"},\"parse_status\":\"ok\","
                    + "\"data\":{\"video_id\":\"dQw4w9WgXcQ\",\"title\":\"Never Gonna Give You Up\","
                    + "\"view_count\":1600000000,\"transcript\":null,\"tags\":[\"rick\",\"astley\"]}}")));

        DataResult out = client.data(DataOptions.builder()
            .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s")
            .country("de")
            .transcript(true)
            .transcriptLanguage("en")
            .param("comments", false)
            .param("max_items", 25)
            .param("a&b=c", "x&y=z #")
            .build());

        assertThat(lastQueryString()).isEqualTo("api_key=test-key"
            + "&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ%26t%3D10s"
            + "&country=de&transcript=true&transcript_language=en"
            + "&comments=false&max_items=25&a%26b%3Dc=x%26y%3Dz%20%23");
        verify(getRequestedFor(urlPathEqualTo("/data"))
            .withQueryParam("url", equalTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
            .withQueryParam("a&b=c", equalTo("x&y=z #")));

        assertThat(out.getParseStatus()).isEqualTo("ok");
        assertThat(out.getRequestParameters().getProvider()).isEqualTo("youtube");
        assertThat(out.getRequestParameters().getType()).isEqualTo("video");
        assertThat(out.getRequestParameters().getUrl()).isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertThat(out.getData().get("title").asText()).isEqualTo("Never Gonna Give You Up");
        assertThat(out.getData().get("view_count").asLong()).isEqualTo(1600000000L);
        assertThat(out.getData().get("transcript").isNull()).isTrue();
        assertThat(out.getData().get("tags")).hasSize(2);
    }

    @Test
    void dataSendsFalseTranscriptAndOmitsUnsetParams() {
        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse().withStatus(200).withBody("{\"parse_status\":\"ok\",\"data\":{}}")));

        client.data(DataOptions.builder().url("https://www.tiktok.com/@nasa").transcript(false).build());
        assertThat(lastQueryString())
            .isEqualTo("api_key=test-key&url=https%3A%2F%2Fwww.tiktok.com%2F%40nasa&transcript=false");

        client.data(DataOptions.builder().url("https://www.tiktok.com/@nasa").build());
        // No scraping params (js, proxy, timeout, ...) and no unset optionals leak in.
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&url=https%3A%2F%2Fwww.tiktok.com%2F%40nasa");
    }

    @Test
    void dataSendsUnknownSiteUrlUnmodifiedWithoutClientSideError() {
        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"request_parameters\":{\"url\":\"https://example.com/anything\","
                    + "\"provider\":\"newsite\",\"type\":\"widget\"},\"parse_status\":\"ok\","
                    + "\"data\":{\"title\":\"x\"}}")));

        // Mixed case, %2F, non-ASCII, space, fragment, surrounding spaces: any lower-casing,
        // trimming, fragment dropping or double-encoding would change what reaches the server.
        String hostile = "  https://Example.COM/A%2Fb/\u00fcn\u00ef?x=1&y=a b#Frag  ";
        DataResult out = client.data(DataOptions.builder().url(hostile).build());

        verify(getRequestedFor(urlPathEqualTo("/data")).withQueryParam("url", equalTo(hostile)));
        assertThat(lastQueryString()).isEqualTo("api_key=test-key&url=%20%20https%3A%2F%2FExample.COM%2FA%252Fb%2F%C3%BCn%C3%AF%3Fx%3D1%26y%3Da%20b%23Frag%20%20");
        // Unknown provider/type strings round-trip; they are not enums.
        assertThat(out.getRequestParameters().getProvider()).isEqualTo("newsite");
        assertThat(out.getRequestParameters().getType()).isEqualTo("widget");
    }

    @Test
    void dataParsesNullDataAndUnknownParseStatus() {
        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse().withStatus(200)
                .withBody("{\"request_parameters\":{\"url\":\"https://www.reddit.com/r/x/comments/1/y/\","
                    + "\"provider\":\"reddit\",\"type\":\"post\"},\"parse_status\":\"parse_failed\","
                    + "\"data\":null,\"extra_future_field\":1}")));

        DataResult out = client.data(DataOptions.builder().url("https://www.reddit.com/r/x/comments/1/y/").build());
        assertThat(out.getParseStatus()).isEqualTo("parse_failed");
        assertThat(out.getData()).isNull();

        stubFor(get(urlPathEqualTo("/data"))
            .willReturn(aResponse().withStatus(200).withBody("{\"parse_status\":\"partially_cached\"}")));
        DataResult absent = client.data(DataOptions.builder().url("https://x.com/nasa").build());
        assertThat(absent.getParseStatus()).isEqualTo("partially_cached");
        assertThat(absent.getData()).isNull();
        assertThat(absent.getRequestParameters()).isNull();
    }
}
