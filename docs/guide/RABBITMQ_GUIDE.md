# RabbitMQ Guide

This document provides essential RabbitMQ concepts and constraints for developers who are new to RabbitMQ message broker.

## Table of Contents
- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [Exchange Types](#exchange-types)
- [MQTT Integration](#mqtt-integration)
- [Constraints and Limitations](#constraints-and-limitations)
- [Best Practices](#best-practices)

## Overview

RabbitMQ is a message broker that implements the Advanced Message Queuing Protocol (AMQP). It acts as an intermediary for messaging, allowing applications to communicate through message passing rather than direct connections.

### Key Benefits
- **Decoupling**: Publishers and consumers don't need to know about each other
- **Reliability**: Messages are persisted and can survive broker restarts
- **Scalability**: Can handle high message throughput
- **Flexibility**: Multiple routing patterns through different exchange types

## Core Concepts

### Basic Components

1. **Producer**: Application that sends messages
2. **Consumer**: Application that receives messages
3. **Queue**: Buffer that stores messages
4. **Exchange**: Routes messages to queues based on rules
5. **Binding**: Link between exchange and queue with routing rules
6. **Routing Key**: Message attribute used for routing decisions

### Message Flow
```
Producer → Exchange → Binding → Queue → Consumer
```

## Exchange Types

RabbitMQ provides different exchange types for various routing patterns:

### 1. Direct Exchange

![Direct Exchange](etc/image/rabbit-direct-exchange.png)

**How it works**:
- Routes messages to queues based on exact routing key match
- One-to-one relationship between routing key and queue
- Most straightforward routing mechanism

**Use cases**:
- Point-to-point communication
- Task distribution to specific workers
- Alert routing to specific handlers

**Example**:
```
Routing Key: "error.payment" → Queue: "payment-error-queue"
Routing Key: "info.user"     → Queue: "user-info-queue"
```

### 2. Fanout Exchange

![Fanout Exchange](etc/image/rabbit-fanout-exchange.png)

**How it works**:
- Broadcasts messages to ALL bound queues
- Ignores routing key completely
- Fastest exchange type

**Use cases**:
- Broadcasting notifications to all subscribers
- Cache invalidation across multiple services
- System-wide announcements

**Example**:
```
Message → Fanout Exchange → Queue A (Email Service)
                        → Queue B (SMS Service)
                        → Queue C (Push Notification Service)
```

### 3. Topic Exchange

![Topic Exchange](etc/image/rabbit-topic-exchange.png)

**How it works**:
- Routes messages based on wildcard pattern matching
- Routing key uses dot-separated words
- Supports `*` (one word) and `#` (zero or more words) wildcards

**Use cases**:
- Hierarchical message routing
- Logging systems with different severity levels
- Regional or categorical message distribution

**Pattern Examples**:
```
Routing Key: "alerts.cpu.critical"
Patterns:
- "alerts.*.*"        → Matches (all alerts)
- "alerts.cpu.#"      → Matches (all CPU alerts)
- "*.cpu.critical"    → Matches (critical CPU from any source)
- "alerts.memory.*"   → No match (different resource type)
```

### 4. Headers Exchange

![Headers Exchange](etc/image/rabbit-header-exchange.png)

**How it works**:
- Routes messages based on message headers instead of routing key
- Uses "x-match" header with "all" or "any" matching logic
- More flexible but slower than other exchange types

**Use cases**:
- Complex routing based on multiple criteria
- Messages with rich metadata
- Conditional routing based on message properties

**Example**:
```
Message Headers: { "type": "alert", "severity": "high", "region": "us-east" }
Binding Headers: { "type": "alert", "severity": "high", "x-match": "all" }
→ Matches because all specified headers match
```

## MQTT Integration

![RabbitMQ MQTT Relation](etc/image/rabbit-mqtt-relation.png)

### MQTT Plugin Benefits
- **Protocol Bridge**: Enables MQTT clients to publish to AMQP infrastructure
- **Lightweight**: MQTT is designed for IoT and resource-constrained environments
- **Persistent Sessions**: Supports durable subscriptions
- **Quality of Service**: Multiple QoS levels for different reliability needs

### Integration Flow in Our Project
1. **Grafana** sends alert webhooks via MQTT protocol
2. **RabbitMQ MQTT Plugin** receives MQTT messages
3. **MQTT Exchange** routes messages to appropriate queues
4. **Application** consumes messages via AMQP for reliable processing

### MQTT vs AMQP
| Feature | MQTT | AMQP |
|---------|------|------|
| **Protocol Size** | Lightweight | Feature-rich |
| **Overhead** | Low | Higher |
| **Reliability** | Basic | Advanced |
| **Routing** | Topic-based | Flexible exchanges |
| **Use Case** | IoT, Simple messaging | Enterprise messaging |

## Constraints and Limitations

### Performance Constraints

1. **Message Size Limits**
   - Default max message size: 128MB
   - Large messages impact performance
   - Consider message chunking for large payloads

2. **Queue Length Limits**
   - Memory usage increases with queue length
   - Configure max-length policies to prevent memory exhaustion
   - Consider lazy queues for large message backlogs

3. **Connection Limits**
   - Default max connections: 65535
   - Each connection consumes memory
   - Use connection pooling in applications

### Durability Constraints

1. **Message Persistence**
   - Only durable queues persist messages to disk
   - Transient queues lose messages on broker restart
   - Persistent messages have performance overhead

2. **Queue Durability**
   - Durable queues survive broker restarts
   - Non-durable queues are lost on restart
   - Choose based on reliability requirements

### Ordering Constraints

1. **Message Ordering**
   - Ordering guaranteed only within single queue
   - Multiple consumers may process messages out of order
   - Use single consumer for strict ordering

2. **Routing Consistency**
   - Exchange type affects message distribution
   - Fanout doesn't guarantee order across queues
   - Consider sequence numbers for ordering requirements

### Network Constraints

1. **Network Partitions**
   - RabbitMQ clusters can split during network issues
   - Configure appropriate partition handling policies
   - Consider network reliability in deployment

2. **Heartbeat Settings**
   - Default heartbeat: 60 seconds
   - Adjust based on network latency and reliability
   - Too frequent heartbeats increase network overhead

## Best Practices

### Queue Management
```bash
# Declare durable queue with TTL
rabbitmqctl declare queue name=alert-queue durable=true arguments='{"x-message-ttl":3600000}'

# Set queue length limit
rabbitmqctl set_policy TTL "^alert" '{"max-length":10000}' --apply-to queues
```

### Connection Management
- Use connection pooling
- Implement proper connection retry logic
- Handle connection failures gracefully

### Message Design
- Keep messages small and focused
- Use appropriate exchange types for routing needs
- Include correlation IDs for request-response patterns

### Monitoring
- Monitor queue lengths and consumer lag
- Track message rates and processing times
- Set up alerts for queue growth and consumer failures

### Error Handling
- Implement dead letter queues for failed messages
- Use message acknowledgments appropriately
- Design retry mechanisms with exponential backoff

## Project Implementation Strategy

This section explains how RabbitMQ is strategically configured and used in the MC-O11Y Trigger project.

### Architecture Overview

The project implements a sophisticated messaging architecture that handles the flow from Grafana alerts to notification delivery:

```
Grafana MQTT → MQTT Exchange → Direct Exchange → Alert Queue → Consumer → Notification
                    ↓ (on failure)
                Dead Letter Exchange → Dead Letter Queue → Recovery Processing
```

### Exchange Strategy

#### 1. MQTT Topic Exchange → Direct Exchange Bridge

```java
// MQTT Exchange for receiving webhooks from Grafana
@Bean
public TopicExchange mqttExchange() {
    return new TopicExchange("mqtt.topic");
}

// Direct Exchange for internal alert processing
@Bean
public DirectExchange alertExchange() {
    return new DirectExchange("alert.exchange");
}

// Bridge between MQTT and internal processing
@Bean
public Binding mqttExToAlertEx() {
    return BindingBuilder.bind(alertExchange()).to(mqttExchange()).with("alert");
}
```

**Strategic Intent**:
- **Protocol Decoupling**: Separate MQTT ingress from internal AMQP processing
- **Routing Flexibility**: Topic exchange handles pattern-based routing from external sources
- **Internal Simplicity**: Direct exchange ensures deterministic routing for internal processing

#### 2. Dead Letter Queue Strategy

```java
@Bean
public Queue alertQueue() {
    return QueueBuilder.durable("alert.queue")
            .withArgument("x-dead-letter-exchange", "alert.dlx")
            .withArgument("x-dead-letter-routing-key", "alert.dlq")
            .build();
}
```

**Strategic Intent**:
- **Reliability**: Failed messages are preserved rather than lost
- **Recovery**: Manual inspection and reprocessing of failed alerts
- **Monitoring**: Dead letter queue metrics indicate system health

### Consumer Configuration Strategy

#### Manual Acknowledgment Pattern

```java
factory.setAcknowledgeMode(MANUAL);
factory.setPrefetchCount(1);
```

**Strategic Intent**:
- **Message Safety**: Acknowledgment only after successful processing
- **Backpressure Control**: Single message prefetch prevents overwhelming consumers
- **Error Isolation**: Failed messages don't block subsequent processing

#### Concurrency Strategy

```java
factory.setConcurrentConsumers(3);
factory.setMaxConcurrentConsumers(10);
```

**Strategic Intent**:
- **Baseline Performance**: 3 concurrent consumers for normal load
- **Burst Handling**: Scale up to 10 consumers during alert storms
- **Resource Management**: Controlled scaling prevents resource exhaustion

### Message Processing Strategy

#### Differentiated Processing

```java
@RabbitListener(queues = "alert.queue", errorHandler = "jsonParseErrorHandler")
public void consumeAlert(@Payload GrafanaAlertMessage alertInfo, ...) {
    if (isTestAlert(alertInfo)) {
        alertEventService.createTestHistory(objectMapper.writeValueAsString(alertInfo));
        channel.basicAck(tag, false);
    }
    // Normal alert processing...
}
```

**Strategic Intent**:
- **Test Isolation**: Test alerts handled separately from production alerts
- **Fast Path**: Early return for test messages reduces processing overhead
- **Audit Trail**: All test alerts are logged for debugging

#### Error Handling Strategy

```java
try {
    // Process alert
    channel.basicAck(tag, false);
} catch (Exception e) {
    log.error("Error while send alert", e);
    channel.basicNack(tag, false, false); // Reject without requeue
}
```

**Strategic Intent**:
- **Fail Fast**: Immediate rejection of unprocessable messages
- **Dead Letter Routing**: Failed messages go to DLQ for analysis
- **Circuit Breaking**: Prevents infinite retry loops

### JSON Processing Strategy

#### Centralized Conversion

```java
@Bean
public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
}

@Bean
public RabbitListenerErrorHandler jsonParseErrorHandler() {
    return (rawMessage, channel, convertedMessage, e) -> {
        if (e.getCause() instanceof MessageConversionException) {
            log.error("Failed to convert message.", e);
            channel.basicAck(rawMessage.getMessageProperties().getDeliveryTag(), false);
        }
        throw e;
    };
}
```

**Strategic Intent**:
- **Type Safety**: Automatic JSON to POJO conversion
- **Error Isolation**: JSON parsing errors don't crash the consumer
- **Data Integrity**: Malformed messages are acknowledged and logged

### Configuration Properties Strategy

#### Environment-Based Configuration

```yaml
spring:
  rabbitmq:
    alert:
      queueName: alert.queue
      exchangeName: alert.exchange
      routingKey: alert
      deadLetterQueueName: alert.dlq
      deadLetterExchangeName: alert.dlx
      deadLetterRoutingKey: alert.dlq
      mqttExchangeName: mqtt.topic
```

**Strategic Intent**:
- **Environment Flexibility**: Different configurations for dev/staging/prod
- **Operational Clarity**: Clear naming conventions for monitoring
- **Deployment Safety**: No hardcoded values in code

### Business Logic Integration

#### Service Layer Delegation

```java
ThresholdCondition thresholdCondition = alertEventService.getThresholdCondition(
    alertInfo.getCommonLabels().get("alertname"));
AlertEvent alertEvent = AlertEvent.from(alertInfo, thresholdCondition);
if (!alertEvent.isEmpty()) {
    alertEventService.createHistory(alertEvent);
    alertEventService.sendNoti(alertEvent);
}
```

**Strategic Intent**:
- **Clean Architecture**: Consumer focuses on message handling, not business logic
- **Testability**: Business logic can be tested independently
- **Maintainability**: Changes to alert processing don't affect messaging code

### Performance Optimizations

#### Message Batching Prevention

```java
factory.setPrefetchCount(1);
```

**Strategic Intent**:
- **Memory Management**: Prevents consumer from buffering large numbers of messages
- **Fair Distribution**: Ensures even load distribution across consumers
- **Latency Optimization**: Messages processed immediately upon availability

#### Connection Efficiency

```java
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
}
```

**Strategic Intent**:
- **Connection Reuse**: Single template instance shares connections
- **Consistent Serialization**: Same JSON converter for all operations
- **Resource Efficiency**: Managed connection lifecycle

### Monitoring and Observability

#### Comprehensive Logging

```java
log.info("Consume alert from alert.queue");
log.debug("AlertInfo: {}", objectMapper.writeValueAsString(alertInfo));
log.debug("Headers: {}", objectMapper.writeValueAsString(headers));
```

**Strategic Intent**:
- **Operational Visibility**: Info logs for message flow tracking
- **Debugging Support**: Debug logs with full message content
- **Audit Trail**: Complete record of message processing

### Key Design Decisions

1. **Hybrid Exchange Strategy**: MQTT Topic → Direct Exchange bridge balances external flexibility with internal predictability
2. **Manual Acknowledgment**: Ensures message durability at the cost of complexity
3. **Dead Letter Queues**: Prioritizes reliability over performance
4. **Low Prefetch Count**: Optimizes for latency over throughput
5. **JSON-First Design**: Simplifies integration with web-based systems
6. **Environment-Driven Config**: Supports multiple deployment environments

## Conclusion

Understanding these RabbitMQ concepts and constraints is crucial for building reliable messaging systems. The MC-O11Y Trigger project demonstrates production-ready patterns for alert processing with emphasis on reliability, observability, and maintainability.

Choose the appropriate exchange type based on your routing needs, consider durability requirements, and implement proper error handling for production systems.

For more detailed information, refer to the [official RabbitMQ documentation](https://www.rabbitmq.com/documentation.html).