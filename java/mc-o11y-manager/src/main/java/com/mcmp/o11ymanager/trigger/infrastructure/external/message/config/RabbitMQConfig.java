package com.mcmp.o11ymanager.trigger.infrastructure.external.message.config;

import static org.springframework.amqp.core.AcknowledgeMode.MANUAL;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ messaging Sets up exchanges, queues, bindings, and message
 * converters for alert processing.
 */
@Slf4j
@EnableRabbit
@Configuration
public class RabbitMQConfig {

  @Value("${spring.rabbitmq.alert.queueName}")
  private String queueName;

  @Value("${spring.rabbitmq.alert.exchangeName}")
  private String exchangeName;

  @Value("${spring.rabbitmq.alert.routingKey}")
  private String routingKey;

  @Value("${spring.rabbitmq.alert.deadLetterQueueName}")
  private String deadLetterQueueName;

  @Value("${spring.rabbitmq.alert.deadLetterExchangeName}")
  private String deadLetterExchangeName;

  @Value("${spring.rabbitmq.alert.deadLetterRoutingKey}")
  private String deadLetterRoutingKey;

  @Value("${spring.rabbitmq.alert.mqttExchangeName}")
  private String mqttExchangeName;

  @Value("${spring.rabbitmq.direct.queueName}")
  private String directQueueName;

  @Value("${spring.rabbitmq.direct.exchangeName}")
  private String directExchangeName;

  @Value("${spring.rabbitmq.direct.routingKey}")
  private String directRoutingKey;

  @Value("${spring.rabbitmq.direct.deadLetterQueueName}")
  private String directDeadLetterQueueName;

  @Value("${spring.rabbitmq.direct.deadLetterExchangeName}")
  private String directDeadLetterExchangeName;

  @Value("${spring.rabbitmq.direct.deadLetterRoutingKey}")
  private String directDeadLetterRoutingKey;


  /**
   * Creates a topic exchange for MQTT messages.
   *
   * @return TopicExchange for MQTT
   */
  @Bean
  public TopicExchange mqttExchange() {
    return new TopicExchange(mqttExchangeName);
  }

  /**
   * Creates a direct exchange for alert messages.
   *
   * @return DirectExchange for alerts
   */
  @Bean
  public DirectExchange alertExchange() {
    return new DirectExchange(exchangeName);
  }

  /**
   * Creates a binding from MQTT exchange to alert exchange.
   *
   * @return Binding between MQTT and alert exchanges
   */
  @Bean
  public Binding mqttExToAlertEx() {
    return BindingBuilder.bind(alertExchange()).to(mqttExchange()).with(routingKey);
  }

  /**
   * Creates the main alert queue with dead letter configuration.
   *
   * @return Queue for processing alerts
   */
  @Bean
  public Queue alertQueue() {
    return QueueBuilder.durable(queueName)
        .withArgument("x-dead-letter-exchange", deadLetterExchangeName)
        .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
        .build();
  }

  /**
   * Creates a binding from alert exchange to alert queue.
   *
   * @return Binding for alert queue
   */
  @Bean
  public Binding alertBinding() {
    return BindingBuilder.bind(alertQueue()).to(alertExchange()).with(routingKey);
  }

  /**
   * Creates the dead letter queue for failed messages.
   *
   * @return Dead letter queue
   */
  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(deadLetterQueueName).build();
  }

  /**
   * Creates a direct exchange for dead letter messages.
   *
   * @return DirectExchange for dead letters
   */
  @Bean
  public DirectExchange deadLetterExchange() {
    return new DirectExchange(deadLetterExchangeName);
  }

  /**
   * Creates a binding from dead letter exchange to dead letter queue.
   *
   * @return Binding for dead letter queue
   */
  @Bean
  public Binding deadLetterBinding() {
    return BindingBuilder.bind(deadLetterQueue())
        .to(deadLetterExchange())
        .with(deadLetterRoutingKey);
  }

  /**
   * Creates a JSON message converter for RabbitMQ.
   *
   * @return Jackson2JsonMessageConverter for JSON serialization
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * Creates a RabbitTemplate with JSON message converter.
   *
   * @param connectionFactory the connection factory
   * @return RabbitTemplate for sending messages
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }

  /**
   * Creates a container factory for RabbitMQ listeners with manual acknowledgment.
   *
   * @param connectionFactory the connection factory
   * @return SimpleRabbitListenerContainerFactory for message listeners
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    factory.setConcurrentConsumers(3);
    factory.setMaxConcurrentConsumers(10);
    factory.setPrefetchCount(1);
    factory.setAcknowledgeMode(MANUAL);
    return factory;
  }

  /**
   * Creates an error handler for JSON parsing errors in RabbitMQ listeners.
   *
   * @return RabbitListenerErrorHandler for handling JSON conversion errors
   */
  @Bean
  public RabbitListenerErrorHandler jsonParseErrorHandler() {
    return (amqpMessage, message, exception) -> {
      if (exception.getCause() instanceof MessageConversionException) {
        log.error("Failed to convert message.", exception);
      }
      throw exception;
    };
  }

  @Bean
  public DirectExchange directExchange() {
    return new DirectExchange(directExchangeName);
  }

  @Bean
  public Queue directQueue() {
    return QueueBuilder.durable(directQueueName)
        .withArgument("x-dead-letter-exchange", directDeadLetterExchangeName)
        .withArgument("x-dead-letter-routing-key", directDeadLetterRoutingKey)
        .build();
  }

  @Bean
  public Binding directBinding() {
    return BindingBuilder.bind(directQueue()).to(directExchange()).with(directRoutingKey);
  }

  @Bean
  public Queue directDeadLetterQueue() {
    return QueueBuilder.durable(directDeadLetterQueueName).build();
  }

  @Bean
  public DirectExchange directDeadLetterExchange() {
    return new DirectExchange(directDeadLetterExchangeName);
  }

  @Bean
  public Binding directDeadLetterBinding() {
    return BindingBuilder.bind(directDeadLetterQueue())
        .to(directDeadLetterExchange())
        .with(directDeadLetterRoutingKey);
  }
}
