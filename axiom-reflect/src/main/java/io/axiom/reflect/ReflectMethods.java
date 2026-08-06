package io.axiom.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectMethods {

    private final Reflect parent;
    private final List<Method> methods;
    private final List<Predicate<Method>> filters;

    public ReflectMethods(Reflect parent) {
        this.parent = parent;
        this.methods = collectAllMethods(parent.type());
        this.filters = new ArrayList<>();
    }

    private static List<Method> collectAllMethods(Class<?> type) {
        List<Method> all = new ArrayList<>();
        // Stop before Object: its declared methods include JDK-internal members (e.g.
        // registerNatives) that setAccessible cannot open without --add-opens, and reflecting
        // them was never the intent of "give me this business class's methods" anyway.
        while (type != null && type != Object.class) {
            for (Method m : type.getDeclaredMethods()) {
                m.setAccessible(true);
                all.add(m);
            }
            type = type.getSuperclass();
        }
        return all;
    }

    public ReflectMethods filter(Predicate<Method> predicate) {
        filters.add(predicate);
        return this;
    }

    public ReflectMethods named(String name) {
        return filter(m -> m.getName().equals(name));
    }

    public ReflectMethods match(String regex) {
        return filter(m -> m.getName().matches(regex));
    }

    public ReflectMethods startWith(String prefix) {
        return filter(m -> m.getName().startsWith(prefix));
    }

    public ReflectMethods getters() {
        return filter(m -> (m.getName().startsWith("get") || m.getName().startsWith("is"))
                && m.getParameterCount() == 0
                && !m.getReturnType().equals(Void.TYPE));
    }

    public ReflectMethods setters() {
        return filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1);
    }

    public ReflectMethods returning(Class<?> type) {
        return filter(m -> m.getReturnType().equals(type));
    }

    public ReflectMethods voidOnly() {
        return filter(m -> m.getReturnType().equals(Void.TYPE));
    }

    public ReflectMethods params(int count) {
        return filter(m -> m.getParameterCount() == count);
    }

    public ReflectMethods noParams() {
        return params(0);
    }

    public ReflectMethods hasParams() {
        return filter(m -> m.getParameterCount() > 0);
    }

    public ReflectMethods annotated(Class<? extends Annotation> annotation) {
        return filter(m -> m.isAnnotationPresent(annotation));
    }

    public ReflectMethods notAnnotated(Class<? extends Annotation> annotation) {
        return filter(m -> !m.isAnnotationPresent(annotation));
    }

    public ReflectMethods isPublic() {
        return filter(m -> Modifier.isPublic(m.getModifiers()));
    }

    public ReflectMethods isPrivate() {
        return filter(m -> Modifier.isPrivate(m.getModifiers()));
    }

    public ReflectMethods isProtected() {
        return filter(m -> Modifier.isProtected(m.getModifiers()));
    }

    public ReflectMethods isStatic() {
        return filter(m -> Modifier.isStatic(m.getModifiers()));
    }

    public ReflectMethods isAbstract() {
        return filter(m -> Modifier.isAbstract(m.getModifiers()));
    }

    public ReflectMethods isFinal() {
        return filter(m -> Modifier.isFinal(m.getModifiers()));
    }

    public ReflectMethods notStatic() {
        return filter(m -> !Modifier.isStatic(m.getModifiers()));
    }

    public ReflectMethods notAbstract() {
        return filter(m -> !Modifier.isAbstract(m.getModifiers()));
    }

    public ReflectMethods notFinal() {
        return filter(m -> !Modifier.isFinal(m.getModifiers()));
    }

    public ReflectMethods each(Consumer<Method> action) {
        stream().forEach(action);
        return this;
    }

    public ReflectMethods eachWrapped(Consumer<ReflectMethod> action) {
        stream().forEach(m -> action.accept(new ReflectMethod(parent, m.getName()).types(m.getParameterTypes())));
        return this;
    }

    public ReflectMethods invokeAll() {
        Object instance = parent.object();
        stream().forEach(m -> {
            try {
                m.invoke(instance);
            } catch (Exception e) {
                throw new ReflectException("Failed to invoke: " + m.getName(), e);
            }
        });
        return this;
    }

    public List<Method> list() {
        return stream().collect(Collectors.toList());
    }

    public List<String> names() {
        return stream().map(Method::getName).collect(Collectors.toList());
    }

    public int count() {
        return (int) stream().count();
    }

    public boolean exists() {
        return count() > 0;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public Optional<Method> first() {
        return stream().findFirst();
    }

    public Optional<ReflectMethod> firstWrapped() {
        return first().map(m -> new ReflectMethod(parent, m.getName()).types(m.getParameterTypes()));
    }

    public ReflectMethods clear() {
        filters.clear();
        return this;
    }

    public Reflect end() {
        return parent;
    }

    private Stream<Method> stream() {
        return methods.stream().filter(m -> filters.stream().allMatch(p -> p.test(m)));
    }
}
