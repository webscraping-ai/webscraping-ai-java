package ai.webscraping.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The {@code request_parameters} block of a {@link DataResult}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DataRequestParameters {

    private final String url;
    private final String provider;
    private final String type;

    public DataRequestParameters(
        @JsonProperty("url") String url,
        @JsonProperty("provider") String provider,
        @JsonProperty("type") String type
    ) {
        this.url = url;
        this.provider = provider;
        this.type = type;
    }

    /** The URL as requested. */
    public String getUrl() {
        return url;
    }

    /** The site the page belongs to, e.g. {@code toscrape.com}. An open set. */
    public String getProvider() {
        return provider;
    }

    /** The kind of page, e.g. {@code product}. Also an open set. */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "DataRequestParameters{url=" + url + ", provider=" + provider + ", type=" + type + "}";
    }
}
