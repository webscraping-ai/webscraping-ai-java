package ai.webscraping.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Account quota information returned by {@code GET /account}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AccountInfo {

    private final String email;
    private final long remainingApiCalls;
    private final Long resetsAt;
    private final Integer remainingConcurrency;

    public AccountInfo(
        @JsonProperty("email") String email,
        @JsonProperty("remaining_api_calls") long remainingApiCalls,
        @JsonProperty("resets_at") Long resetsAt,
        @JsonProperty("remaining_concurrency") Integer remainingConcurrency
    ) {
        this.email = email;
        this.remainingApiCalls = remainingApiCalls;
        this.resetsAt = resetsAt;
        this.remainingConcurrency = remainingConcurrency;
    }

    /** The account's email address. */
    public String getEmail() {
        return email;
    }

    /** Number of API calls remaining in the current billing period. */
    public long getRemainingApiCalls() {
        return remainingApiCalls;
    }

    /** Unix-time (seconds) when the API-call allowance resets, or {@code null}. */
    public Long getResetsAt() {
        return resetsAt;
    }

    /** Number of concurrent requests still available, or {@code null}. */
    public Integer getRemainingConcurrency() {
        return remainingConcurrency;
    }

    @Override
    public String toString() {
        return "AccountInfo{email=" + email
            + ", remainingApiCalls=" + remainingApiCalls
            + ", resetsAt=" + resetsAt
            + ", remainingConcurrency=" + remainingConcurrency + "}";
    }
}
