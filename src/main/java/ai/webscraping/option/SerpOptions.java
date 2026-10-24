package ai.webscraping.option;

/**
 * Options for {@link ai.webscraping.Client#serp(SerpOptions)}.
 *
 * <p>{@code /serp} is query-shaped, not URL-shaped: none of the page-scraping
 * options ({@code js}, {@code proxy}, {@code country}, …) apply, so this class
 * deliberately does not extend {@link CommonOptions}. Unset fields are dropped
 * from the wire and the server applies its own defaults.
 *
 * <p>Every search runs from one fixed location: {@code location},
 * {@code lat} + {@code lon} or {@code uule}, otherwise the capital of the
 * {@code gl} country, so repeats and later pages are consistent. Combining
 * them, or {@code radius} without a location, is rejected by the server with
 * a 400 (not billed); the client does not check it.
 */
public final class SerpOptions {

    private final String q;
    private final String engine;
    private final String gl;
    private final String hl;
    private final Integer page;
    private final Integer pages;
    private final String location;
    private final String uule;
    private final Double lat;
    private final Double lon;
    private final Integer radius;

    private SerpOptions(Builder b) {
        this.q = b.q;
        this.engine = b.engine;
        this.gl = b.gl;
        this.hl = b.hl;
        this.page = b.page;
        this.pages = b.pages;
        this.location = b.location;
        this.uule = b.uule;
        this.lat = b.lat;
        this.lon = b.lon;
        this.radius = b.radius;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The search query. Required and must not be blank; sent as given (not trimmed). */
    public String getQ() {
        return q;
    }

    /** Search engine to query ({@code google}, the default, is the only one today), or {@code null}. */
    public String getEngine() {
        return engine;
    }

    /**
     * Two-letter country code for geolocation of the search, or {@code null}. The API defaults it
     * to the country of {@code location}, {@code lat}/{@code lon} or {@code uule}, otherwise {@code us}.
     */
    public String getGl() {
        return gl;
    }

    /** Two-letter language code for the results (API default {@code en}), or {@code null}. */
    public String getHl() {
        return hl;
    }

    /**
     * Results page number, starting at 1 (API default 1, 10 results per page), or
     * {@code null}. Values below 1 are rejected by the client; the server rejects a
     * page above 100 with a 400 (not billed).
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Number of consecutive results pages to fetch in one call, starting at
     * {@link #getPage()}, or {@code null} (API default 1). The pages come back
     * as one de-duplicated list, billed per page returned. Values below 1 are
     * rejected by the client; the server rejects more than 5 (or running past
     * page 100) with a 400 (not billed).
     */
    public Integer getPages() {
        return pages;
    }

    /**
     * Where the search runs from, or {@code null}: a canonical name such as
     * {@code Austin,Texas,United States}, a looser name such as
     * {@code Austin, TX}, or an id from
     * {@link ai.webscraping.Client#serpLocations(SerpLocationsOptions)}. Sent as
     * given; an unknown name is a 400 (not billed).
     */
    public String getLocation() {
        return location;
    }

    /** Google-encoded location ({@code w+CAIQICI…} or {@code a+…}), or {@code null}. Sent as given. */
    public String getUule() {
        return uule;
    }

    /**
     * Latitude of the search origin in decimal degrees, or {@code null}. Requires
     * {@link #getLon()}. Sent as a plain decimal rounded to 7 fractional digits.
     */
    public Double getLat() {
        return lat;
    }

    /**
     * Longitude of the search origin in decimal degrees, or {@code null}. Requires
     * {@link #getLat()}. Sent as a plain decimal rounded to 7 fractional digits.
     */
    public Double getLon() {
        return lon;
    }

    /**
     * Radius in meters around the location or {@code lat}/{@code lon}, or
     * {@code null}. Values below 1 are rejected by the client; the server
     * rejects more than 1000 with a 400 (not billed).
     */
    public Integer getRadius() {
        return radius;
    }

    public static final class Builder {
        private String q;
        private String engine;
        private String gl;
        private String hl;
        private Integer page;
        private Integer pages;
        private String location;
        private String uule;
        private Double lat;
        private Double lon;
        private Integer radius;

        public Builder q(String q) {
            this.q = q;
            return this;
        }

        public Builder engine(String engine) {
            this.engine = engine;
            return this;
        }

        public Builder gl(String gl) {
            this.gl = gl;
            return this;
        }

        public Builder hl(String hl) {
            this.hl = hl;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pages(int pages) {
            this.pages = pages;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder uule(String uule) {
            this.uule = uule;
            return this;
        }

        public Builder lat(double lat) {
            this.lat = lat;
            return this;
        }

        public Builder lon(double lon) {
            this.lon = lon;
            return this;
        }

        public Builder radius(int radius) {
            this.radius = radius;
            return this;
        }

        public SerpOptions build() {
            return new SerpOptions(this);
        }
    }
}
