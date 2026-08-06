package io.axiom.yaml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** An ordered YAML sequence. */
public final class YamlSequence extends YamlNode implements Iterable<YamlNode> {

    private final List<YamlNode> elements = new ArrayList<>();

    public void add(YamlNode element) {
        elements.add(element == null ? YamlNull.INSTANCE : element);
    }

    public YamlNode get(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Iterator<YamlNode> iterator() {
        return elements.iterator();
    }

    @Override
    public YamlSequence deepCopy() {
        YamlSequence copy = new YamlSequence();
        for (YamlNode element : elements) {
            copy.add(element.deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YamlSequence other && elements.equals(other.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
