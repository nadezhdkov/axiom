package io.axiom.yaml;

/**
 * Base of the Axiom YAML tree model, mirroring {@code axiom-json}'s {@code JsonElement}
 * architecture. Never touches the underlying engine — SnakeYAML is hidden behind
 * {@code internal.snakeyaml.*}.
 */
public abstract sealed class YamlNode permits YamlMapping, YamlSequence, YamlScalar, YamlNull {

    public boolean isMapping() {
        return this instanceof YamlMapping;
    }

    public boolean isSequence() {
        return this instanceof YamlSequence;
    }

    public boolean isScalar() {
        return this instanceof YamlScalar;
    }

    public boolean isNull() {
        return this instanceof YamlNull;
    }

    public YamlMapping asMapping() {
        if (this instanceof YamlMapping mapping) {
            return mapping;
        }
        throw new IllegalStateException("Not a YamlMapping: " + this);
    }

    public YamlSequence asSequence() {
        if (this instanceof YamlSequence sequence) {
            return sequence;
        }
        throw new IllegalStateException("Not a YamlSequence: " + this);
    }

    public YamlScalar asScalar() {
        if (this instanceof YamlScalar scalar) {
            return scalar;
        }
        throw new IllegalStateException("Not a YamlScalar: " + this);
    }

    public abstract YamlNode deepCopy();
}
