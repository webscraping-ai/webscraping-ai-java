package ai.webscraping.option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Options for {@link ai.webscraping.Client#selectedMultiple(SelectedMultipleOptions)}. */
public final class SelectedMultipleOptions extends CommonOptions {

    private final String url;
    private final List<String> selectors;

    private SelectedMultipleOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.selectors = b.selectors == null ? null : Collections.unmodifiableList(new ArrayList<>(b.selectors));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public static final class Builder extends BaseBuilder<SelectedMultipleOptions, Builder> {
        private String url;
        private List<String> selectors;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder selectors(List<String> selectors) {
            this.selectors = selectors == null ? null : new ArrayList<>(selectors);
            return this;
        }

        public Builder selectors(String... selectors) {
            this.selectors = selectors == null ? null : new ArrayList<>(Arrays.asList(selectors));
            return this;
        }

        public Builder addSelector(String selector) {
            if (this.selectors == null) {
                this.selectors = new ArrayList<>();
            }
            this.selectors.add(selector);
            return this;
        }

        @Override
        public SelectedMultipleOptions build() {
            return new SelectedMultipleOptions(this);
        }
    }
}
