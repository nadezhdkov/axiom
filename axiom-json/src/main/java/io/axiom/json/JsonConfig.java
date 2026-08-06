package io.axiom.json;

/** Immutable configuration for a {@link JsonMapper}. */
public final class JsonConfig {

    public enum AnnotationsMode {
        AXIOM_ONLY, NONE
    }

    private static final JsonConfig DEFAULT = builder().build();

    private final boolean prettyPrint;
    private final boolean serializeNulls;
    private final boolean lenient;
    private final boolean failOnUnknownFields;
    private final boolean annotationsEnabled;
    private final boolean htmlEscaping;
    private final String dateFormat;
    private final AnnotationsMode annotationsMode;

    private JsonConfig(Builder builder) {
        this.prettyPrint = builder.prettyPrint;
        this.serializeNulls = builder.serializeNulls;
        this.lenient = builder.lenient;
        this.failOnUnknownFields = builder.failOnUnknownFields;
        this.annotationsEnabled = builder.annotationsEnabled;
        this.htmlEscaping = builder.htmlEscaping;
        this.dateFormat = builder.dateFormat;
        this.annotationsMode = builder.annotationsMode;
    }

    public static JsonConfig defaultConfig() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public boolean isSerializeNulls() {
        return serializeNulls;
    }

    public boolean isLenient() {
        return lenient;
    }

    public boolean isFailOnUnknownFields() {
        return failOnUnknownFields;
    }

    public boolean isAnnotationsEnabled() {
        return annotationsEnabled;
    }

    public boolean isHtmlEscaping() {
        return htmlEscaping;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public AnnotationsMode getAnnotationsMode() {
        return annotationsMode;
    }

    public static final class Builder {
        private boolean prettyPrint = false;
        private boolean serializeNulls = false;
        private boolean lenient = false;
        private boolean failOnUnknownFields = false;
        private boolean annotationsEnabled = true;
        private boolean htmlEscaping = true;
        private String dateFormat = null;
        private AnnotationsMode annotationsMode = AnnotationsMode.AXIOM_ONLY;

        private Builder() {
        }

        public Builder prettyPrint(boolean value) {
            this.prettyPrint = value;
            return this;
        }

        public Builder serializeNulls(boolean value) {
            this.serializeNulls = value;
            return this;
        }

        public Builder lenient(boolean value) {
            this.lenient = value;
            return this;
        }

        public Builder failOnUnknownFields(boolean value) {
            this.failOnUnknownFields = value;
            return this;
        }

        public Builder annotationsEnabled(boolean value) {
            this.annotationsEnabled = value;
            return this;
        }

        public Builder htmlEscaping(boolean value) {
            this.htmlEscaping = value;
            return this;
        }

        public Builder dateFormat(String value) {
            this.dateFormat = value;
            return this;
        }

        public Builder annotationsMode(AnnotationsMode value) {
            this.annotationsMode = value;
            return this;
        }

        public JsonConfig build() {
            return new JsonConfig(this);
        }

        public JsonMapper buildMapper() {
            return Json.mapper(build());
        }
    }
}
