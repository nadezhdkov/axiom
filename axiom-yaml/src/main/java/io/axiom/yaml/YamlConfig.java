package io.axiom.yaml;

/** Immutable configuration for a {@link YamlMapper}. */
public final class YamlConfig {

    private static final YamlConfig DEFAULT = builder().build();

    private final boolean prettyFlowStyle;
    private final int indent;
    private final boolean annotationsEnabled;

    private YamlConfig(Builder builder) {
        this.prettyFlowStyle = builder.prettyFlowStyle;
        this.indent = builder.indent;
        this.annotationsEnabled = builder.annotationsEnabled;
    }

    public static YamlConfig defaultConfig() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isPrettyFlowStyle() {
        return prettyFlowStyle;
    }

    public int getIndent() {
        return indent;
    }

    public boolean isAnnotationsEnabled() {
        return annotationsEnabled;
    }

    public static final class Builder {
        private boolean prettyFlowStyle = false;
        private int indent = 2;
        private boolean annotationsEnabled = true;

        private Builder() {
        }

        public Builder prettyFlowStyle(boolean value) {
            this.prettyFlowStyle = value;
            return this;
        }

        public Builder indent(int value) {
            this.indent = value;
            return this;
        }

        public Builder annotationsEnabled(boolean value) {
            this.annotationsEnabled = value;
            return this;
        }

        public YamlConfig build() {
            return new YamlConfig(this);
        }

        public YamlMapper buildMapper() {
            return Yaml.mapper(build());
        }
    }
}
