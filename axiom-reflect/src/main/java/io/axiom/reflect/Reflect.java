package io.axiom.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent entry point for reflective access: field/method access, annotations, and object
 * construction, without hand-written try/catch around checked reflection exceptions.
 */
public class Reflect {

    private final Class<?> type;
    private Object object;

    private Reflect(Class<?> type) {
        this.type = type;
    }

    private Reflect(Object object) {
        this.object = object;
        this.type = object.getClass();
    }

    public static Reflect on(Class<?> type) {
        if (type == null) throw new IllegalArgumentException("Class cannot be null");
        return new Reflect(type);
    }

    public static Reflect on(Object object) {
        if (object == null) throw new IllegalArgumentException("Instance cannot be null");
        return new Reflect(object);
    }

    @SuppressWarnings("unchecked")
    public <T> T create() {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            throw new ReflectException("Failed to create instance of " + type.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Object... args) {
        try {
            Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
            var constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new ReflectException("Failed to create instance with args", e);
        }
    }

    public Field field(String name) {
        return new Field(this, name);
    }

    public Fields fields() {
        return new Fields(this);
    }

    public ReflectMethod method(String name) {
        return new ReflectMethod(this, name);
    }

    public ReflectMethods methods() {
        return new ReflectMethods(this);
    }

    public ReflectAnnotations annotations() {
        return new ReflectAnnotations(type);
    }

    public Class<?> type() {
        return type;
    }

    public Object object() {
        return object;
    }

    public Reflect bind(Object object) {
        this.object = object;
        return this;
    }

    public boolean isInterface() {
        return type.isInterface();
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(type.getModifiers());
    }

    public boolean isEnum() {
        return type.isEnum();
    }

    public boolean isAnnotation() {
        return type.isAnnotation();
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return type.getAnnotation(annotation);
    }

    public Reflect superclass() {
        Class<?> parent = type.getSuperclass();
        if (parent == null) throw new ReflectException("No superclass found for " + type.getName());
        return new Reflect(parent);
    }

    public List<Class<?>> interfaces() {
        return Arrays.asList(type.getInterfaces());
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }
}
