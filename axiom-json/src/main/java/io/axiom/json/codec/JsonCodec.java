package io.axiom.json.codec;

import io.axiom.json.JsonElement;

/** A pluggable, hand-written converter between {@code T} and the JSON tree model. */
public interface JsonCodec<T> {

    JsonElement encode(T value);

    T decode(JsonElement element);
}
