package dev.volkang.eventbus.async;

import dev.volkang.eventbus.EventBus;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous event dispatcher that processes events
 * on a configurable thread pool.
 *
 * <p>Usage:
 * <pre>
 * var dispatcher = AsyncEventDispatcher.builder()
 *     .corePoolSize(4)
 *     .maxPoolSize(16)
 *     .queueCapacity(1000)
 *     .build(eventBus);
 *
 * dispatcher.dispatch(new OrderCreatedEvent(orderId));
 * </pre>
 */
public class AsyncEventDispatcher {

    private static final Logger LOG = Logger.getLogger(AsyncEventDispatcher.class.getName());

    private final EventBus eventBus;
    private final ExecutorService executor;
    private final int queueCapacity;

    private AsyncEventDispatcher(EventBus eventBus, ExecutorService executor, int queueCapacity) {
        this.eventBus = eventBus;
        this.executor = executor;
        this.queueCapacity = queueCapacity;
    }

    /**
     * Dispatch an event asynchronously.
     * Returns a Future that completes when all handlers finish.
     */
    public <T> CompletableFuture<Void> dispatch(T event) {
        return CompletableFuture.runAsync(() -> {
            try {
                eventBus.publish(event);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error dispatching event: " + event.getClass().getSimpleName(), e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Dispatch an event and block until all handlers complete.
     */
    public <T> void dispatchAndWait(T event, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        dispatch(event).get(timeout, unit);
    }

    /**
     * Gracefully shut down the thread pool.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int corePoolSize = 2;
        private int maxPoolSize = 8;
        private int queueCapacity = 500;
        private String threadNamePrefix = "eventbus-async-";

        public Builder corePoolSize(int size) { this.corePoolSize = size; return this; }
        public Builder maxPoolSize(int size) { this.maxPoolSize = size; return this; }
        public Builder queueCapacity(int capacity) { this.queueCapacity = capacity; return this; }
        public Builder threadNamePrefix(String prefix) { this.threadNamePrefix = prefix; return this; }

        public AsyncEventDispatcher build(EventBus eventBus) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueCapacity);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize,
                60L, TimeUnit.SECONDS,
                queue,
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, threadNamePrefix + counter++);
                        t.setDaemon(true);
                        return t;
                    }
                }
            );
            return new AsyncEventDispatcher(eventBus, executor, queueCapacity);
        }
    }
}
