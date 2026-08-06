package io.axiom.yaml.internal.snakeyaml;

import io.axiom.reflect.Reflect;
import io.axiom.yaml.YamlMapping;
import io.axiom.yaml.YamlNode;
import io.axiom.yaml.YamlNull;
import io.axiom.yaml.YamlScalar;
import io.axiom.yaml.YamlSequence;
import io.axiom.yaml.annotations.YamlDefault;
import io.axiom.yaml.annotations.YamlIgnore;
import io.axiom.yaml.annotations.YamlName;
import io.axiom.yaml.annotations.YamlRequired;
import io.axiom.yaml.error.YamlMappingException;
import io.axiom.yaml.error.YamlValidationException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive object &lt;-&gt; {@link YamlNode} binder, entirely independent of SnakeYAML's own
 * (much more limited) POJO binding — this is what lets {@code @YamlName}/{@code @YamlIgnore}/
 * {@code @YamlDefault}/{@code @YamlRequired} be enforced consistently, mirroring the
 * {@code axiom-json} annotation contract instead of duplicating a second incompatible one.
 */
public final class ObjectBinder {

    private final boolean annotationsEnabled;

    public ObjectBinder(boolean annotationsEnabled) {
        this.annotationsEnabled = annotationsEnabled;
    }

    public YamlNode encode(Object value, Type declaredType) {
        if (value == null) {
            return YamlNull.INSTANCE;
        }
        if (value instanceof String string) {
            return new YamlScalar(string);
        }
        if (value instanceof Number number) {
            return new YamlScalar(number);
        }
        if (value instanceof Boolean bool) {
            return new YamlScalar(bool);
        }
        if (value instanceof Enum<?> enumValue) {
            return new YamlScalar(enumValue.name());
        }
        if (value instanceof Iterable<?> iterable) {
            Type elementType = elementType(declaredType, 0);
            YamlSequence sequence = new YamlSequence();
            for (Object element : iterable) {
                sequence.add(encode(element, elementType));
            }
            return sequence;
        }
        if (value instanceof Map<?, ?> map) {
            Type valueType = elementType(declaredType, 1);
            YamlMapping mapping = new YamlMapping();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                mapping.add(String.valueOf(entry.getKey()), encode(entry.getValue(), valueType));
            }
            return mapping;
        }
        return encodePojo(value);
    }

    private YamlNode encodePojo(Object value) {
        YamlMapping mapping = new YamlMapping();
        for (Field field : Reflect.on(value.getClass()).fields().list()) {
            if (annotationsEnabled && field.isAnnotationPresent(YamlIgnore.class)) {
                continue;
            }
            try {
                Object fieldValue = field.get(value);
                mapping.add(serializedName(field), encode(fieldValue, field.getGenericType()));
            } catch (IllegalAccessException e) {
                throw new YamlMappingException("Failed to read field " + field.getName()
                        + " of " + value.getClass().getName(), e);
            }
        }
        return mapping;
    }

    @SuppressWarnings("unchecked")
    public <T> T decode(YamlNode node, Type targetType) {
        if (targetType instanceof Class<?> rawClass) {
            return (T) decodeToClass(node, rawClass);
        }
        if (targetType instanceof ParameterizedType parameterized) {
            Class<?> rawClass = (Class<?>) parameterized.getRawType();
            if (List.class.isAssignableFrom(rawClass)) {
                return (T) decodeList(node, parameterized.getActualTypeArguments()[0]);
            }
            if (Map.class.isAssignableFrom(rawClass)) {
                return (T) decodeMap(node, parameterized.getActualTypeArguments()[1]);
            }
        }
        throw new YamlMappingException("Unsupported target type: " + targetType);
    }

    private List<Object> decodeList(YamlNode node, Type elementType) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<Object> result = new ArrayList<>();
        for (YamlNode element : node.asSequence()) {
            result.add(decode(element, elementType));
        }
        return result;
    }

    private Map<String, Object> decodeMap(YamlNode node, Type valueType) {
        if (node == null || node.isNull()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, YamlNode> entry : node.asMapping().entrySet()) {
            result.put(entry.getKey(), decode(entry.getValue(), valueType));
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object decodeToClass(YamlNode node, Class<?> type) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (type == String.class) {
            return node.asScalar().asString();
        }
        if (type == int.class || type == Integer.class) {
            return node.asScalar().asInt();
        }
        if (type == long.class || type == Long.class) {
            return node.asScalar().asLong();
        }
        if (type == double.class || type == Double.class) {
            return node.asScalar().asDouble();
        }
        if (type == float.class || type == Float.class) {
            return (float) node.asScalar().asDouble();
        }
        if (type == boolean.class || type == Boolean.class) {
            return node.asScalar().asBoolean();
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, node.asScalar().asString());
        }
        return decodePojo(node.asMapping(), type);
    }

    private Object decodePojo(YamlMapping mapping, Class<?> type) {
        Object instance = Reflect.on(type).create();
        for (Field field : Reflect.on(type).fields().list()) {
            if (annotationsEnabled && field.isAnnotationPresent(YamlIgnore.class)) {
                continue;
            }
            String name = serializedName(field);
            YamlNode value = mapping.get(name);
            boolean missing = value == null || value.isNull();
            if (missing && annotationsEnabled) {
                YamlDefault defaultValue = field.getAnnotation(YamlDefault.class);
                if (defaultValue != null) {
                    setField(field, instance, decodeToClass(new YamlScalar(defaultValue.value()), field.getType()));
                    continue;
                }
                if (field.isAnnotationPresent(YamlRequired.class)) {
                    throw new YamlValidationException(
                            "Required key \"" + name + "\" is missing or null for " + type.getName());
                }
            }
            if (!missing) {
                setField(field, instance, decode(value, field.getGenericType()));
            }
        }
        return instance;
    }

    private void setField(Field field, Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new YamlMappingException("Failed to set field " + field.getName()
                    + " of " + instance.getClass().getName(), e);
        }
    }

    private String serializedName(Field field) {
        if (!annotationsEnabled) {
            return field.getName();
        }
        YamlName name = field.getAnnotation(YamlName.class);
        return name != null ? name.value() : field.getName();
    }

    private static Type elementType(Type declaredType, int index) {
        if (declaredType instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length > index) {
            return parameterized.getActualTypeArguments()[index];
        }
        return Object.class;
    }
}
