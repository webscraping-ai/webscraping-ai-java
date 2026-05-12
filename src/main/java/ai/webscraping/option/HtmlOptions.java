package ai.webscraping.option;

/** Options for {@link ai.webscraping.Client#html(HtmlOptions)}. */
public final class HtmlOptions extends CommonOptions {

    private final String url;
    private final String format;
    private final Boolean returnScriptResult;

    private HtmlOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.format = b.format;
        this.returnScriptResult = b.returnScriptResult;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public Boolean getReturnScriptResult() {
        return returnScriptResult;
    }

    public static final class Builder extends BaseBuilder<HtmlOptions, Builder> {
        private String url;
        private String format;
        private Boolean returnScriptResult;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder returnScriptResult(boolean returnScriptResult) {
            this.returnScriptResult = returnScriptResult;
            return this;
        }

        @Override
        public HtmlOptions build() {
            return new HtmlOptions(this);
        }
    }
}
