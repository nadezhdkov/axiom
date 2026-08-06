package io.axiom.yaml;

import java.util.Objects;

/** A YAML string, number, or boolean leaf value. */
public final class YamlScalar extends YamlNode {

    private final Object value;

    public YamlScalar(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public YamlScalar(Number value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public YamlScalar(Boolean value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public String asString() {
        return value instanceof Boolean bool ? bool.toString() : value.toString();
    }

    public Number asNumber() {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String string) {
            return new java.math.BigDecimal(string);
        }
        throw new UnsupportedOperationException("Not a number: " + value);
    }

    public int asInt() {
        return asNumber().intValue();
    }

    public long asLong() {
        return asNumber().longValue();
    }

    public double asDouble() {
        return asNumber().doubleValue();
    }

    public boolean asBoolean() {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(asString());
    }

    Object rawValue() {
        return value;
    }

    @Override
    public YamlNode deepCopy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlScalar other)) {
            return false;
        }
        if (value instanceof Number thisNumber && other.value instanceof Number otherNumber) {
            return thisNumber.doubleValue() == otherNumber.doubleValue();
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value instanceof Number number ? Long.hashCode(number.longValue()) : value.hashCode();
    }

    @Override
    public String toString() {
        return asString();
    }
}
