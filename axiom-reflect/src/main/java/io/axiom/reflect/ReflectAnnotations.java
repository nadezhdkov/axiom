package io.axiom.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReflectAnnotations {

    private final AnnotatedElement element;

    public ReflectAnnotations(AnnotatedElement element) {
        this.element = element;
    }

    public boolean has(Class<? extends Annotation> type) {
        return element.isAnnotationPresent(type);
    }

    public <T extends Annotation> T get(Class<T> type) {
        return element.getAnnotation(type);
    }

    public <T extends Annotation> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(element.getAnnotation(type));
    }

    public List<Annotation> list() {
        return Arrays.asList(element.getAnnotations());
    }

    public <T extends Annotation> List<T> list(Class<T> type) {
        return Arrays.asList(element.getAnnotationsByType(type));
    }

    public Map<String, Annotation> map() {
        return Arrays.stream(element.getAnnotations())
                .collect(Collectors.toMap(a -> a.annotationType().getSimpleName(), a -> a));
    }

    public List<String> names() {
        return Arrays.stream(element.getAnnotations())
                .map(a -> a.annotationType().getSimpleName())
                .collect(Collectors.toList());
    }

    public List<Class<? extends Annotation>> types() {
        return Arrays.stream(element.getAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toList());
    }

    public int count() {
        return element.getAnnotations().length;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public <T> T value(Class<? extends Annotation> type) {
        return value(type, "value");
    }

    @SuppressWarnings("unchecked")
    public <T> T value(Class<? extends Annotation> type, String member) {
        Annotation annotation = get(type);
        if (annotation == null) return null;

        try {
            var method = annotation.annotationType().getMethod(member);
            return (T) method.invoke(annotation);
        } catch (Exception e) {
            throw new ReflectException("Failed to read value from annotation @" + type.getSimpleName(), e);
        }
    }
}
