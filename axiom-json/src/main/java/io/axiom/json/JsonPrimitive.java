package io.axiom.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** A JSON string, number, or boolean leaf value. */
public final class JsonPrimitive extends JsonElement {

    private final Object value;

    public JsonPrimitive(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public JsonPrimitive(Number value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public JsonPrimitive(Boolean value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public JsonPrimitive(Character value) {
        Objects.requireNonNull(value, "value must not be null");
        this.value = value.toString();
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
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        return value.toString();
    }

    public Number getAsNumber() {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String string) {
            return new BigDecimal(string);
        }
        throw new UnsupportedOperationException("Not a number: " + value);
    }

    public boolean getAsBoolean() {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(asString());
    }

    public int asInt() {
        return getAsNumber().intValue();
    }

    public long asLong() {
        return getAsNumber().longValue();
    }

    public double asDouble() {
        return getAsNumber().doubleValue();
    }

    public float asFloat() {
        return getAsNumber().floatValue();
    }

    public byte asByte() {
        return getAsNumber().byteValue();
    }

    public short asShort() {
        return getAsNumber().shortValue();
    }

    public BigInteger asBigInteger() {
        return getAsNumber() instanceof BigInteger bigInteger ? bigInteger : BigInteger.valueOf(asLong());
    }

    public BigDecimal asBigDecimal() {
        return getAsNumber() instanceof BigDecimal bigDecimal ? bigDecimal : new BigDecimal(getAsNumber().toString());
    }

    @Override
    public JsonElement deepCopy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JsonPrimitive other)) {
            return false;
        }
        if (value instanceof Number thisNumber && other.value instanceof Number otherNumber) {
            return thisNumber.doubleValue() == otherNumber.doubleValue();
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        if (value instanceof Number number) {
            return Long.hashCode(number.longValue());
        }
        return value.hashCode();
    }

    @Override
    public String toString() {
        return asString();
    }
}
