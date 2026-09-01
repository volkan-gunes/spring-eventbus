package dev.volkangunes.eventbus;

/**
 * Functional interface for handling events.
 */
@FunctionalInterface
public interface EventHandler<T> {
    void handle(T event) throws Exception;
}
