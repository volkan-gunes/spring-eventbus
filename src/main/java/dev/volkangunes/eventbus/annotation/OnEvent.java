package dev.volkangunes.eventbus.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as an event handler.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnEvent {
    Class<?> value();
}
