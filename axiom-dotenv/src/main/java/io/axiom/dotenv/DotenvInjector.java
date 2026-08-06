package io.axiom.dotenv;

import io.axiom.dotenv.internal.DotenvTypeConverter;
import io.axiom.reflect.Reflect;

/**
 * Injects {@link Dotenv} values into {@code @Env}-annotated fields of a target object, using
 * {@code axiom-reflect} internally instead of raw {@code java.lang.reflect.Field} — the
 * reference implementation this was ported from re-implemented field access by hand instead of
 * reusing its own reflection module.
 */
final class DotenvInjector {

    private final Dotenv dotenv;

    DotenvInjector(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    void inject(Object target) {
        Class<?> clazz = target.getClass();
        String prefix = prefixOf(clazz);

        Reflect.on(target).fields().each(field -> {
            if (field.isAnnotationPresent(EnvIgnore.class) || !field.isAnnotationPresent(Env.class)) {
                return;
            }

            String key = prefix + field.getAnnotation(Env.class).value();
            String rawValue = resolveValue(field, key);
            if (rawValue == null) {
                return;
            }

            try {
                Object converted = DotenvTypeConverter.convert(rawValue, field.getType());
                Reflect.on(target).field(field.getName()).set(converted);
            } catch (Exception e) {
                throw new DotenvInjectionException(
                        "Failed to inject field '" + field.getName() + "' (key: " + key
                                + ", value: \"" + rawValue + "\", target type: "
                                + field.getType().getSimpleName() + "): " + e.getMessage(), e);
            }
        });
    }

    private String resolveValue(java.lang.reflect.Field field, String key) {
        String value = dotenv.get(key);

        if (value == null && field.isAnnotationPresent(Default.class)) {
            value = field.getAnnotation(Default.class).value();
        }
        if (value == null && field.isAnnotationPresent(RequiredEnv.class)) {
            RequiredEnv required = field.getAnnotation(RequiredEnv.class);
            String message = required.message().isEmpty() ? "Missing required env var: " + key : required.message();
            throw new DotenvInjectionException(message);
        }
        return value;
    }

    private static String prefixOf(Class<?> clazz) {
        EnvPrefix prefix = clazz.getAnnotation(EnvPrefix.class);
        return prefix != null ? prefix.value() : "";
    }
}
