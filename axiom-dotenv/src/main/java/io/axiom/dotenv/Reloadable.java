package io.axiom.dotenv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a target class into {@link DotenvBinder#reload(Object, Dotenv)}. Required so that
 * re-injecting a live object is always an explicit, deliberate choice by whoever owns that
 * object's class — never something that can happen to an arbitrary bound object by accident.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Reloadable {
}
