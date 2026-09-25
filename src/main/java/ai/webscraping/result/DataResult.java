package ai.webscraping.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structured page data returned by {@code GET /data}.
 *
 * <p>{@code provider}, {@code type} and {@code parseStatus} are plain strings,
 * not enums: new sites and page types are added on the server, so unknown
 * values come through unchanged. {@link #getData()} is untyped JSON because its
 * shape depends on the provider and page type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DataResult {

    private final DataRequestParameters requestParameters;
    private final String parseStatus;
    private final JsonNode data;

    public DataResult(
        @JsonProperty("request_parameters") DataRequestParameters requestParameters,
        @JsonProperty("parse_status") String parseStatus,
        @JsonProperty("data") JsonNode data
    ) {
        this.requestParameters = requestParameters;
        this.parseStatus = parseStatus;
        this.data = data == null || data.isNull() ? null : data;
    }

    /** The URL as requested and how the server classified it. */
    public DataRequestParameters getRequestParameters() {
        return requestParameters;
    }

    /**
     * {@code ok}, {@code parse_failed} (fetched but not parsed; data may be
     * null or partial) or {@code not_found} today. All three are successful,
     * charged requests. Treat as an open set.
     */
    public String getParseStatus() {
        return parseStatus;
    }

    /**
     * The page's fields as a Jackson tree (snake_case keys; fields the page
     * doesn't expose are JSON null), or {@code null} when the API returned
     * {@code data: null}. Convert to your own type with
     * {@code new ObjectMapper().treeToValue(result.getData(), MyType.class)}.
     */
    public JsonNode getData() {
        return data;
    }

    @Override
    public String toString() {
        return "DataResult{requestParameters=" + requestParameters
            + ", parseStatus=" + parseStatus
            + ", data=" + data + "}";
    }
}
