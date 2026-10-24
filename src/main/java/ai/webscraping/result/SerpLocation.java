package ai.webscraping.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One location returned by {@code GET /serp/locations}. Pass
 * {@link #getCanonicalName()} (or {@link #getId()}) as
 * {@link ai.webscraping.option.SerpOptions.Builder#location(String)}.
 *
 * <p>Optional response fields are returned as {@code null} when the API omits them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SerpLocation {

    private final String id;
    private final Long googleId;
    private final Long googleParentId;
    private final String name;
    private final String canonicalName;
    private final String countryCode;
    private final String targetType;
    private final Long reach;
    private final List<Double> gps;

    public SerpLocation(
        @JsonProperty("id") String id,
        @JsonProperty("google_id") Long googleId,
        @JsonProperty("google_parent_id") Long googleParentId,
        @JsonProperty("name") String name,
        @JsonProperty("canonical_name") String canonicalName,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("target_type") String targetType,
        @JsonProperty("reach") Long reach,
        @JsonProperty("gps") List<Double> gps
    ) {
        this.id = id;
        this.googleId = googleId;
        this.googleParentId = googleParentId;
        this.name = name;
        this.canonicalName = canonicalName;
        this.countryCode = countryCode;
        this.targetType = targetType;
        this.reach = reach;
        this.gps = gps == null ? null : Collections.unmodifiableList(new ArrayList<>(gps));
    }

    /** Location id (a string, e.g. {@code "1026201"}), usable as the {@code /serp} {@code location}. */
    public String getId() {
        return id;
    }

    /** Google geo target id. */
    public Long getGoogleId() {
        return googleId;
    }

    /** Google geo target id of the parent location, or {@code null} (e.g. for countries). */
    public Long getGoogleParentId() {
        return googleParentId;
    }

    /** Short name, e.g. {@code Austin}. */
    public String getName() {
        return name;
    }

    /** Full name to pass as {@code location}, e.g. {@code Austin,Texas,United States}. */
    public String getCanonicalName() {
        return canonicalName;
    }

    /** Two-letter country code, e.g. {@code US}. */
    public String getCountryCode() {
        return countryCode;
    }

    /** Google's target type, e.g. {@code City}, {@code State} or {@code Country}. An open set, not an enum. */
    public String getTargetType() {
        return targetType;
    }

    /** Approximate population, used for ordering. */
    public Long getReach() {
        return reach;
    }

    /** Coordinates the search is sent from, as {@code [longitude, latitude]}, or {@code null}. */
    public List<Double> getGps() {
        return gps;
    }

    @Override
    public String toString() {
        return "SerpLocation{id=" + id + ", googleId=" + googleId + ", googleParentId=" + googleParentId
            + ", name=" + name + ", canonicalName=" + canonicalName + ", countryCode=" + countryCode
            + ", targetType=" + targetType + ", reach=" + reach + ", gps=" + gps + "}";
    }
}
