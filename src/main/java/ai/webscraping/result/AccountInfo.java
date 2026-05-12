package ai.webscraping.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Account quota information returned by {@code GET /account}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AccountInfo {

    private final String email;
    private final long remainingApiCalls;
    private final Long resumesAt;
    private final String concurrencyLimit;
    private final String creditsPerMonth;

    public AccountInfo(
        @JsonProperty("email") String email,
        @JsonProperty("remaining_api_calls") long remainingApiCalls,
        @JsonProperty("resumes_at") Long resumesAt,
        @JsonProperty("concurrency_limit") String concurrencyLimit,
        @JsonProperty("credits_per_month") String creditsPerMonth
    ) {
        this.email = email;
        this.remainingApiCalls = remainingApiCalls;
        this.resumesAt = resumesAt;
        this.concurrencyLimit = concurrencyLimit;
        this.creditsPerMonth = creditsPerMonth;
    }

    public String getEmail() {
        return email;
    }

    public long getRemainingApiCalls() {
        return remainingApiCalls;
    }

    /** Unix-time (seconds) when API credits refill, or {@code null}. */
    public Long getResumesAt() {
        return resumesAt;
    }

    public String getConcurrencyLimit() {
        return concurrencyLimit;
    }

    public String getCreditsPerMonth() {
        return creditsPerMonth;
    }

    @Override
    public String toString() {
        return "AccountInfo{email=" + email + ", remainingApiCalls=" + remainingApiCalls + "}";
    }
}
