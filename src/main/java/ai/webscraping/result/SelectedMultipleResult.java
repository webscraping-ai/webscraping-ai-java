package ai.webscraping.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@code /selected-multiple} endpoint returns
 * {@code Array<Array<String>>}, not a flat {@code Array<String>} — confirmed
 * against the live API. The outer wrapper is always size 1 in practice
 * but the schema reflects what the server emits, not what the spec implies.
 */
public final class SelectedMultipleResult {

    private final List<List<String>> results;

    public SelectedMultipleResult(List<List<String>> results) {
        if (results == null) {
            this.results = Collections.emptyList();
            return;
        }
        List<List<String>> copy = new ArrayList<>(results.size());
        for (List<String> inner : results) {
            copy.add(inner == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(inner)));
        }
        this.results = Collections.unmodifiableList(copy);
    }

    public List<List<String>> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "SelectedMultipleResult{results=" + results + "}";
    }
}
