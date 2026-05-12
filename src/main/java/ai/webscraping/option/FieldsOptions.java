package ai.webscraping.option;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Options for {@link ai.webscraping.Client#fields(FieldsOptions)}. */
public final class FieldsOptions extends CommonOptions {

    private final String url;
    private final Map<String, String> fields;

    private FieldsOptions(Builder b) {
        super(b);
        this.url = b.url;
        this.fields = b.fields == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(b.fields));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public static final class Builder extends BaseBuilder<FieldsOptions, Builder> {
        private String url;
        private Map<String, String> fields;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder fields(Map<String, String> fields) {
            this.fields = fields == null ? null : new LinkedHashMap<>(fields);
            return this;
        }

        public Builder addField(String name, String description) {
            if (this.fields == null) {
                this.fields = new LinkedHashMap<>();
            }
            this.fields.put(name, description);
            return this;
        }

        @Override
        public FieldsOptions build() {
            return new FieldsOptions(this);
        }
    }
}
