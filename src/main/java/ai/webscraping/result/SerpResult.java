package ai.webscraping.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parsed search engine results returned by {@code GET /serp}. Field names
 * follow the common SERP API naming (the SerpApi family): {@code organic_results}
 * items carry {@code position}, {@code title}, {@code link}, {@code domain},
 * {@code displayed_link}, {@code snippet} and {@code date}.
 *
 * <p>Optional response fields are returned as {@code null} when the API omits them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SerpResult {

    private final SearchParameters searchParameters;
    private final SearchInformation searchInformation;
    private final List<OrganicResult> organicResults;
    private final List<RelatedSearch> relatedSearches;
    private final Pagination pagination;

    public SerpResult(
        @JsonProperty("search_parameters") SearchParameters searchParameters,
        @JsonProperty("search_information") SearchInformation searchInformation,
        @JsonProperty("organic_results") List<OrganicResult> organicResults,
        @JsonProperty("related_searches") List<RelatedSearch> relatedSearches,
        @JsonProperty("pagination") Pagination pagination
    ) {
        this.searchParameters = searchParameters;
        this.searchInformation = searchInformation;
        this.organicResults = organicResults == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(organicResults));
        this.relatedSearches = relatedSearches == null
            ? null
            : Collections.unmodifiableList(new ArrayList<>(relatedSearches));
        this.pagination = pagination;
    }

    /** The normalized parameters the search was run with. */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /** What the engine reported about the search itself. */
    public SearchInformation getSearchInformation() {
        return searchInformation;
    }

    /** Organic (non-ad) results in rank order; empty when the page had none. */
    public List<OrganicResult> getOrganicResults() {
        return organicResults;
    }

    /** "Related searches" suggestions, or {@code null} when the page shows none. */
    public List<RelatedSearch> getRelatedSearches() {
        return relatedSearches;
    }

    /** Current and next page numbers. */
    public Pagination getPagination() {
        return pagination;
    }

    @Override
    public String toString() {
        return "SerpResult{searchParameters=" + searchParameters
            + ", searchInformation=" + searchInformation
            + ", organicResults=" + organicResults
            + ", relatedSearches=" + relatedSearches
            + ", pagination=" + pagination + "}";
    }

    /** The {@code search_parameters} block. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SearchParameters {

        private final String engine;
        private final String q;
        private final String gl;
        private final String hl;
        private final Integer page;

        public SearchParameters(
            @JsonProperty("engine") String engine,
            @JsonProperty("q") String q,
            @JsonProperty("gl") String gl,
            @JsonProperty("hl") String hl,
            @JsonProperty("page") Integer page
        ) {
            this.engine = engine;
            this.q = q;
            this.gl = gl;
            this.hl = hl;
            this.page = page;
        }

        public String getEngine() {
            return engine;
        }

        public String getQ() {
            return q;
        }

        public String getGl() {
            return gl;
        }

        public String getHl() {
            return hl;
        }

        public Integer getPage() {
            return page;
        }

        @Override
        public String toString() {
            return "SearchParameters{engine=" + engine + ", q=" + q + ", gl=" + gl
                + ", hl=" + hl + ", page=" + page + "}";
        }
    }

    /** The {@code search_information} block. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SearchInformation {

        private final String queryDisplayed;
        private final String organicResultsState;
        private final String showingResultsFor;
        private final Long totalResults;

        public SearchInformation(
            @JsonProperty("query_displayed") String queryDisplayed,
            @JsonProperty("organic_results_state") String organicResultsState,
            @JsonProperty("showing_results_for") String showingResultsFor,
            @JsonProperty("total_results") Long totalResults
        ) {
            this.queryDisplayed = queryDisplayed;
            this.organicResultsState = organicResultsState;
            this.showingResultsFor = showingResultsFor;
            this.totalResults = totalResults;
        }

        /** The query the results are for. Equals {@code q} unless the engine applied a spelling fix. */
        public String getQueryDisplayed() {
            return queryDisplayed;
        }

        /**
         * {@code "Results for exact spelling"}, {@code "Empty showing fixed spelling results"}
         * (see {@link #getShowingResultsFor()}), or {@code "Fully empty"} (no organic results;
         * still a successful, billed search).
         */
        public String getOrganicResultsState() {
            return organicResultsState;
        }

        /** The auto-corrected query, or {@code null} when no spelling fix was applied. */
        public String getShowingResultsFor() {
            return showingResultsFor;
        }

        /** The engine's estimated total result count, or {@code null} when not reported. */
        public Long getTotalResults() {
            return totalResults;
        }

        @Override
        public String toString() {
            return "SearchInformation{queryDisplayed=" + queryDisplayed
                + ", organicResultsState=" + organicResultsState
                + ", showingResultsFor=" + showingResultsFor
                + ", totalResults=" + totalResults + "}";
        }
    }

    /** One item of {@code organic_results}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class OrganicResult {

        private final Integer position;
        private final String title;
        private final String link;
        private final String domain;
        private final String displayedLink;
        private final String snippet;
        private final String date;

        public OrganicResult(
            @JsonProperty("position") Integer position,
            @JsonProperty("title") String title,
            @JsonProperty("link") String link,
            @JsonProperty("domain") String domain,
            @JsonProperty("displayed_link") String displayedLink,
            @JsonProperty("snippet") String snippet,
            @JsonProperty("date") String date
        ) {
            this.position = position;
            this.title = title;
            this.link = link;
            this.domain = domain;
            this.displayedLink = displayedLink;
            this.snippet = snippet;
            this.date = date;
        }

        /**
         * Rank within this page, starting at 1 on every page, or {@code null}
         * when the API omits it. Compute {@code (page - 1) * 10 + position}
         * for an absolute rank.
         */
        public Integer getPosition() {
            return position;
        }

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }

        /** Hostname of {@link #getLink()} without a leading {@code www.}. */
        public String getDomain() {
            return domain;
        }

        /** The breadcrumb-style URL shown under the title (falls back to the domain). */
        public String getDisplayedLink() {
            return displayedLink;
        }

        /** Result description snippet, or {@code null} when none is shown. */
        public String getSnippet() {
            return snippet;
        }

        /** The date shown next to the snippet, verbatim (absolute or relative), or {@code null}. */
        public String getDate() {
            return date;
        }

        @Override
        public String toString() {
            return "OrganicResult{position=" + position + ", title=" + title + ", link=" + link
                + ", domain=" + domain + ", displayedLink=" + displayedLink
                + ", snippet=" + snippet + ", date=" + date + "}";
        }
    }

    /** One item of {@code related_searches}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RelatedSearch {

        private final String query;

        public RelatedSearch(@JsonProperty("query") String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }

        @Override
        public String toString() {
            return "RelatedSearch{query=" + query + "}";
        }
    }

    /** The {@code pagination} block. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Pagination {

        private final Integer current;
        private final Integer next;

        public Pagination(
            @JsonProperty("current") Integer current,
            @JsonProperty("next") Integer next
        ) {
            this.current = current;
            this.next = next;
        }

        public Integer getCurrent() {
            return current;
        }

        /** The next page number, or {@code null} when no further page is offered. */
        public Integer getNext() {
            return next;
        }

        @Override
        public String toString() {
            return "Pagination{current=" + current + ", next=" + next + "}";
        }
    }
}
