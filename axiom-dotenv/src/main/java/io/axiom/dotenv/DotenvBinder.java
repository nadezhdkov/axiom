package io.axiom.dotenv;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Entry point for binding {@link Dotenv} values into {@code @Env}-annotated fields. */
public final class DotenvBinder {

    private DotenvBinder() {
    }

    /**
     * Binds {@code target}'s {@code @Env} fields from {@code dotenv}. If {@code target}'s class is
     * annotated with {@link Profile}, binding is skipped entirely (fields left untouched) unless
     * {@link Dotenv#activeProfile()} is present and matches one of the listed profiles.
     */
    public static void bind(Object target, Dotenv dotenv) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(dotenv, "dotenv");
        if (!profileMatches(target.getClass(), dotenv)) {
            return;
        }
        new DotenvInjector(dotenv).inject(target);
    }

    /**
     * Re-reads {@code dotenv} (via {@link Dotenv#reload()}) and re-binds {@code target} against
     * the fresh values. Requires {@code target}'s class to be annotated with {@link Reloadable} —
     * reloading a live object is only ever allowed when that class explicitly opted in.
     *
     * @return the reloaded {@link Dotenv}, so callers can keep using it for later reloads
     */
    public static Dotenv reload(Object target, Dotenv dotenv) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(dotenv, "dotenv");
        if (!target.getClass().isAnnotationPresent(Reloadable.class)) {
            throw new DotenvInjectionException(
                    "Cannot reload " + target.getClass().getName() + ": class is not annotated with @Reloadable");
        }
        Dotenv fresh = dotenv.reload();
        bind(target, fresh);
        return fresh;
    }

    private static boolean profileMatches(Class<?> targetClass, Dotenv dotenv) {
        Profile annotation = targetClass.getAnnotation(Profile.class);
        if (annotation == null) {
            return true;
        }
        Optional<String> active = dotenv.activeProfile();
        return active.isPresent() && Arrays.asList(annotation.value()).contains(active.get());
    }
}
