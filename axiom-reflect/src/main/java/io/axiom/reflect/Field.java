package io.axiom.reflect;

import io.axiom.reflect.internal.LookupCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

public class Field {

    private final Reflect parent;
    private final java.lang.reflect.Field field;

    public Field(Reflect parent, String name) {
        this.parent = parent;
        this.field = LookupCache.field(parent.type(), name, () -> findField(parent.type(), name));
    }

    private static java.lang.reflect.Field findField(Class<?> owner, String name) {
        Class<?> type = owner;
        while (type != null && type != Object.class) {
            try {
                java.lang.reflect.Field f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new ReflectException("Field not found: " + name);
    }

    public <T> T get() {
        return get(parent.object());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object instance) {
        if (instance == null && !isStatic()) {
            throw new ReflectException("Cannot get non-static field without instance");
        }
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new ReflectException("Failed to get field: " + field.getName(), e);
        }
    }

    public Field set(Object value) {
        return set(parent.object(), value);
    }

    public Field set(Object instance, Object value) {
        if (instance == null && !isStatic()) {
            throw new ReflectException("Cannot set non-static field without instance");
        }
        try {
            field.set(instance, value);
            return this;
        } catch (IllegalAccessException e) {
            throw new ReflectException("Failed to set field: " + field.getName(), e);
        }
    }

    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }

    public boolean isFinal() {
        return Modifier.isFinal(field.getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(field.getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(field.getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(field.getModifiers());
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return field.getAnnotation(annotation);
    }

    public Annotation[] getAnnotations() {
        return field.getAnnotations();
    }

    public Class<?> type() {
        return field.getType();
    }

    public String name() {
        return field.getName();
    }

    public boolean isNull() {
        return get() == null;
    }

    public boolean isNotNull() {
        return get() != null;
    }

    public Field copyTo(Field target) {
        return target.set(this.get());
    }

    public Field copyTo(Object targetInstance, String targetName) {
        Object value = this.get();
        Reflect.on(targetInstance).field(targetName).set(value);
        return this;
    }

    public java.lang.reflect.Field unwrap() {
        return field;
    }

    public Reflect end() {
        return parent;
    }
}
