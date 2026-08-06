package io.axiom.yaml;

/** Singleton representing an absent/{@code null} YAML value. */
public final class YamlNull extends YamlNode {

    public static final YamlNull INSTANCE = new YamlNull();

    private YamlNull() {
    }

    @Override
    public YamlNode deepCopy() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YamlNull;
    }

    @Override
    public int hashCode() {
        return YamlNull.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }
}
