package ai.webscraping.option;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options for {@link ai.webscraping.Client#data(DataOptions)}.
 *
 * <p>{@code /data} returns structured JSON for a page on a supported site
 * (e.g. YouTube, TikTok, X, LinkedIn, Instagram, Reddit). It ignores the
 * page-scraping options ({@code js}, {@code proxy}, {@code headers}, …), so
 * this class deliberately does not extend {@link CommonOptions}. Unset fields
 * are dropped from the wire and the server applies its own defaults.
 *
 * <p>The URL is never checked against a list of sites: new sites and page
 * types are added on the server. An unsupported URL or page type returns a
 * 400 ({@link ai.webscraping.exception.BadRequestException}) that is not
 * charged. Its message lists what is supported. Parameters a future provider needs can be
 * passed with {@link Builder#param(String, Object)} without a client release.
 */
public final class DataOptions {

    private final String url;
    private final String country;
    private final Boolean transcript;
    private final String transcriptLanguage;
    private final Map<String, Object> params;

    private DataOptions(Builder b) {
        this.url = b.url;
        this.country = b.country;
        this.transcript = b.transcript;
        this.transcriptLanguage = b.transcriptLanguage;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(b.params));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The page URL. Required and must not be blank; sent exactly as given. */
    public String getUrl() {
        return url;
    }

    /**
     * Two-letter country code of the proxy used to fetch the page, {@code us} by default, or {@code null}.
     * The server checks it against its proxy-country list and rejects unknown ones with a 400.
     */
    public String getCountry() {
        return country;
    }

    /**
     * YouTube videos only. Also fetch the video's transcript into {@code data.transcript}. It's null when
     * no matching captions are available. If the transcript fetch itself fails, the whole request fails
     * with a 500 and is not charged. {@code null} when unset.
     */
    public Boolean getTranscript() {
        return transcript;
    }

    /**
     * Caption language to pick, e.g. {@code en} or {@code de}. Without it, English is preferred, then the
     * first available track. If the video has no captions in that language, {@code data.transcript} is
     * null. {@code null} when unset.
     */
    public String getTranscriptLanguage() {
        return transcriptLanguage;
    }

    /** Extra query parameters sent as-is, in insertion order; empty when none were set. */
    public Map<String, Object> getParams() {
        return params;
    }

    public static final class Builder {
        private String url;
        private String country;
        private Boolean transcript;
        private String transcriptLanguage;
        private final Map<String, Object> params = new LinkedHashMap<>();

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder transcript(boolean transcript) {
            this.transcript = transcript;
            return this;
        }

        public Builder transcriptLanguage(String transcriptLanguage) {
            this.transcriptLanguage = transcriptLanguage;
            return this;
        }

        /**
         * Adds an extra query parameter, sent as-is (percent-encoded like every
         * other parameter). For provider-specific parameters this SDK doesn't
         * model yet. The value must be a {@code String}, {@code Boolean}
         * (sent as {@code true}/{@code false}) or {@code Number}. Setting the
         * same key twice keeps the last value.
         * {@link ai.webscraping.Client#data(DataOptions)} rejects {@code api_key}
         * and {@code url} (any case) and the typed option names
         * {@code country}, {@code transcript} and {@code transcript_language}
         * (use the typed setters instead), whether or not those are set.
         *
         * @throws IllegalArgumentException if the key is null, empty or
         *     whitespace-only, or the value is null or not a String, Boolean or Number
         */
        public Builder param(String key, Object value) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("param key must not be blank");
            }
            if (!(value instanceof String || value instanceof Boolean || value instanceof Number)) {
                throw new IllegalArgumentException(
                    "param " + key + " must be a String, Boolean or Number, got "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            this.params.put(key, value);
            return this;
        }

        /** Adds every entry of {@code params} via {@link #param(String, Object)}. */
        public Builder params(Map<String, ?> params) {
            if (params != null) {
                for (Map.Entry<String, ?> e : params.entrySet()) {
                    param(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public DataOptions build() {
            return new DataOptions(this);
        }
    }
}
