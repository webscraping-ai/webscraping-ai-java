package ai.webscraping.option;

/** Options for {@link ai.webscraping.Client#question(QuestionOptions)}. */
public final class QuestionOptions extends CommonOptions {

    private final String url;
    private final String question;
    private final String format;

    private QuestionOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.question = b.question;
        this.format = b.format;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public String getQuestion() {
        return question;
    }

    public String getFormat() {
        return format;
    }

    public static final class Builder extends BaseBuilder<QuestionOptions, Builder> {
        private String url;
        private String question;
        private String format;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        @Override
        public QuestionOptions build() {
            return new QuestionOptions(this);
        }
    }
}
