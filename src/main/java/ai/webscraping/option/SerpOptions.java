package ai.webscraping.option;

/**
 * Options for {@link ai.webscraping.Client#serp(SerpOptions)}.
 *
 * <p>{@code /serp} is query-shaped, not URL-shaped: none of the page-scraping
 * options ({@code js}, {@code proxy}, {@code country}, …) apply, so this class
 * deliberately does not extend {@link CommonOptions}. Unset fields are dropped
 * from the wire and the server applies its own defaults.
 */
public final class SerpOptions {

    private final String q;
    private final String engine;
    private final String gl;
    private final String hl;
    private final Integer page;

    private SerpOptions(Builder b) {
        this.q = b.q;
        this.engine = b.engine;
        this.gl = b.gl;
        this.hl = b.hl;
        this.page = b.page;
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

    /** Two-letter country code for geolocation of the search (API default {@code us}), or {@code null}. */
    public String getGl() {
        return gl;
    }

    /** Two-letter language code for the results (API default {@code en}), or {@code null}. */
    public String getHl() {
        return hl;
    }

    /**
     * Results page number, starting at 1 (API default 1, 10 results per page), or
     * {@code null}. Values below 1 are rejected by the client; the server caps the
     * page at 100.
     */
    public Integer getPage() {
        return page;
    }

    public static final class Builder {
        private String q;
        private String engine;
        private String gl;
        private String hl;
        private Integer page;

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

        public SerpOptions build() {
            return new SerpOptions(this);
        }
    }
}
