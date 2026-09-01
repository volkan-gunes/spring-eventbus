package dev.volkangunes.eventbus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core event bus for publishing and subscribing to domain events.
 */
public class EventBus {

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();

    /**
     * Register an event handler for a specific event type.
     */
    public <T> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Publish an event to all registered handlers.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<?> handler : eventHandlers) {
                try {
                    ((EventHandler<T>) handler).handle(event);
                } catch (Exception e) {
                    // TODO: Route to dead letter queue
                    throw new EventBusException("Handler failed for event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * Get the count of registered handlers.
     */
    public int handlerCount() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }
}
