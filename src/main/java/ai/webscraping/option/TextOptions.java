package ai.webscraping.option;

/** Options for {@link ai.webscraping.Client#text(TextOptions)}. */
public final class TextOptions extends CommonOptions {

    private final String url;
    private final String textFormat;
    private final Boolean returnLinks;

    private TextOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.textFormat = b.textFormat;
        this.returnLinks = b.returnLinks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public String getTextFormat() {
        return textFormat;
    }

    public Boolean getReturnLinks() {
        return returnLinks;
    }

    public static final class Builder extends BaseBuilder<TextOptions, Builder> {
        private String url;
        private String textFormat;
        private Boolean returnLinks;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder textFormat(String textFormat) {
            this.textFormat = textFormat;
            return this;
        }

        public Builder returnLinks(boolean returnLinks) {
            this.returnLinks = returnLinks;
            return this;
        }

        @Override
        public TextOptions build() {
            return new TextOptions(this);
        }
    }
}
