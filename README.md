# ☕ spring-eventbus

Event-driven microservices framework for Spring Boot with saga orchestration, dead letter handling, and event sourcing support.

[![Maven Central](https://img.shields.io/badge/maven-v2.1.0-C71A36)](https://search.maven.org)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17+-ED8B00.svg)](https://openjdk.org)

## Overview

**spring-eventbus** simplifies building event-driven microservices with Spring Boot. It provides annotations-based event handling, saga orchestration, automatic retry with dead letter queues, and event sourcing patterns.

## Features

- 📨 **Declarative events** — Annotation-based event publishing and subscribing
- 🔄 **Saga orchestration** — Distributed transaction management with compensation
- 💀 **Dead letter handling** — Automatic retry and dead letter queue management
- 📝 **Event sourcing** — Built-in event store with snapshotting
- 🔌 **Multiple transports** — Kafka, RabbitMQ, and in-memory

## Quick Start

```xml
<dependency>
    <groupId>dev.volkangunes</groupId>
    <artifactId>spring-eventbus-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

```java
@EventBusEnabled
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

// Publishing events
@Service
public class OrderService {
    @Autowired
    private EventBus eventBus;

    public Order createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        eventBus.publish(new OrderCreatedEvent(order.getId(), order.getTotal()));
        return order;
    }
}

// Subscribing to events
@EventHandler
public class InventoryEventHandler {

    @OnEvent(OrderCreatedEvent.class)
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleOrderCreated(OrderCreatedEvent event) {
        inventoryService.reserveItems(event.getOrderId());
    }
}
```

## Saga Example

```java
@Saga
public class OrderSaga {

    @SagaStart
    @OnEvent(OrderCreatedEvent.class)
    public void onOrderCreated(OrderCreatedEvent event) {
        send(new ReserveInventoryCommand(event.getOrderId()));
    }

    @SagaStep
    @OnEvent(InventoryReservedEvent.class)
    public void onInventoryReserved(InventoryReservedEvent event) {
        send(new ProcessPaymentCommand(event.getOrderId()));
    }

    @SagaStep
    @Compensate(method = "compensatePayment")
    @OnEvent(PaymentProcessedEvent.class)
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        send(new ConfirmOrderCommand(event.getOrderId()));
    }

    public void compensatePayment(PaymentFailedEvent event) {
        send(new ReleaseInventoryCommand(event.getOrderId()));
        send(new CancelOrderCommand(event.getOrderId()));
    }
}
```

## License

Apache License 2.0
