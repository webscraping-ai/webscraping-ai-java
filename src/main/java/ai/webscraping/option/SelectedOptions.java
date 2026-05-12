package ai.webscraping.option;

/** Options for {@link ai.webscraping.Client#selected(SelectedOptions)}. */
public final class SelectedOptions extends CommonOptions {

    private final String url;
    private final String selector;
    private final String format;

    private SelectedOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.selector = b.selector;
        this.format = b.format;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public String getSelector() {
        return selector;
    }

    public String getFormat() {
        return format;
    }

    public static final class Builder extends BaseBuilder<SelectedOptions, Builder> {
        private String url;
        private String selector;
        private String format;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder selector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        @Override
        public SelectedOptions build() {
            return new SelectedOptions(this);
        }
    }
}
