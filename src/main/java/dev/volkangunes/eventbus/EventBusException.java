package dev.volkangunes.eventbus;

public class EventBusException extends RuntimeException {
    public EventBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
