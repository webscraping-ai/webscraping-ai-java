package ai.webscraping.option;

/**
 * Options for {@link ai.webscraping.Client#serpLocations(SerpLocationsOptions)}.
 * Unset fields are dropped from the wire and the server applies its own defaults.
 */
public final class SerpLocationsOptions {

    private final String q;
    private final Integer limit;

    private SerpLocationsOptions(Builder b) {
        this.q = b.q;
        this.limit = b.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Part of a location name (English), e.g. {@code austin}, or {@code null}
     * to get the most populous locations. Sent as given.
     */
    public String getQ() {
        return q;
    }

    /**
     * Maximum number of locations to return, or {@code null} (API default 10).
     * Values below 1 are rejected by the client; the server rejects more than
     * 10 with a 400.
     */
    public Integer getLimit() {
        return limit;
    }

    public static final class Builder {
        private String q;
        private Integer limit;

        public Builder q(String q) {
            this.q = q;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public SerpLocationsOptions build() {
            return new SerpLocationsOptions(this);
        }
    }
}
