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

    /** The detected site, e.g. {@code youtube} or {@code reddit}. An open set: new sites are added over time. */
    public String getProvider() {
        return provider;
    }

    /** The detected page kind within the site, e.g. {@code video} or {@code profile}. Also an open set. */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "DataRequestParameters{url=" + url + ", provider=" + provider + ", type=" + type + "}";
    }
}
