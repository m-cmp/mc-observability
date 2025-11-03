package com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.persistence.model.DirectAlert;
import com.mcmp.o11ymanager.trigger.application.service.NotiService;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.message.GrafanaAlertMessage;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiSender;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for processing alert events from Grafana Listens to alert queue and dead letter
 * queue, processes alert messages, and delegates to AlertEventService for business logic
 * execution.
 */
@Slf4j
@Component
public class AlertEventConsumer {

  private final AlertEventService alertEventService;
  private final ObjectMapper objectMapper;
  private final NotiSender notiSender;
  private final NotiFactory notiFactory;
  private final NotiService notiService;

  /**
   * Constructor for AlertEventConsumer.
   *
   * @param alertEventService service for processing alert events
   * @param objectMapper      Jackson ObjectMapper for JSON serialization/logging
   */
  public AlertEventConsumer(AlertEventService alertEventService, ObjectMapper objectMapper, NotiSender notiSender, NotiFactory notiFactory, NotiService notiService) {
    this.alertEventService = alertEventService;
    this.objectMapper = objectMapper;
    this.notiSender = notiSender;
    this.notiFactory = notiFactory;
    this.notiService = notiService;
  }

  /**
   * Consumes alert messages from the main alert queue. Processes Grafana alert messages, creates
   * alert events, and sends notifications.
   *
   * @param alertInfo the Grafana alert message payload
   * @param headers   RabbitMQ message headers
   * @param tag       delivery tag for message acknowledgment
   * @param channel   RabbitMQ channel for manual acknowledgment
   * @throws IOException if channel operations fail
   */
  @RabbitListener(queues = "alert.queue", errorHandler = "jsonParseErrorHandler")
  public void consumeAlert(
      @Payload GrafanaAlertMessage alertInfo,
      @Headers Map<String, Object> headers,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
      Channel channel)
      throws IOException {
    log.info("Consume alert from alert.queue");
    log.debug("AlertInfo: {}", objectMapper.writeValueAsString(alertInfo));
    log.debug("Headers: {}", objectMapper.writeValueAsString(headers));

    try {
      if (isTestAlert(alertInfo)) {

        alertEventService.createTestHistory(objectMapper.writeValueAsString(alertInfo));
        channel.basicAck(tag, false);
        return;
      }

      ThresholdCondition thresholdCondition =
          alertEventService.getThresholdCondition(
              alertInfo.getCommonLabels().get("ruleGroup"));
      AlertEvent alertEvent = AlertEvent.from(alertInfo, thresholdCondition);
      if (!alertEvent.isEmpty()) {
        alertEventService.createHistory(alertEvent);
        alertEventService.sendNoti(alertEvent);
      }
      channel.basicAck(tag, false);
    } catch (Exception e) {
      log.error("Error while send alert", e);
      channel.basicNack(tag, false, false);
    }
  }



  @RabbitListener(queues = "direct.queue", errorHandler = "jsonParseErrorHandler")
  public void consumeDirectAlert(
      @Payload DirectAlert directAlert,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
      Channel channel) throws IOException {

    log.info("Consume Direct alert message from direct.queue");
    log.debug("Raw message: {}", objectMapper.writeValueAsString(directAlert));

      try {
        Noti noti = notiFactory.createDirectNoti(directAlert);
        NotiResult result = notiSender.send(noti);
        result.setChannel(directAlert.getChannelName());
        notiService.createNotiHistory(List.of(result));
        channel.basicAck(tag, false);
      } catch (Exception e) {
        log.info("=================================Error while processing Direct alert=================================", e);
        channel.basicNack(tag, false, false);
      }
  }



  /**
   * Handles messages from the dead letter queue. Processes failed alert messages with the same
   * logic as the main queue.
   *
   * @param alertInfo the Grafana alert message payload
   * @param headers   RabbitMQ message headers
   * @param tag       delivery tag for message acknowledgment
   * @param channel   RabbitMQ channel for manual acknowledgment
   * @throws IOException if channel operations fail
   */
  @RabbitListener(queues = "alert.dlq", errorHandler = "jsonParseErrorHandler")
  public void handleDeadLetterMessage(
      @Payload GrafanaAlertMessage alertInfo,
      @Headers Map<String, Object> headers,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
      Channel channel)
      throws IOException {
    log.info("Consume alert from alert.dlq");
    log.debug("AlertInfo: {}", objectMapper.writeValueAsString(alertInfo));
    log.debug("Headers: {}", objectMapper.writeValueAsString(headers));

    try {
      if (isTestAlert(alertInfo)) {

        alertEventService.createTestHistory(objectMapper.writeValueAsString(alertInfo));
        channel.basicAck(tag, false);
        return;
      }

      ThresholdCondition thresholdCondition =
          alertEventService.getThresholdCondition(
              alertInfo.getCommonLabels().get("alertname"));
      AlertEvent alertEvent = AlertEvent.from(alertInfo, thresholdCondition);
      if (!alertEvent.isEmpty()) {
        alertEventService.createHistory(alertEvent);
        alertEventService.sendNoti(alertEvent);
      }
      channel.basicAck(tag, false);
    } catch (Exception e) {
      log.error("Error while send alert", e);
      channel.basicNack(tag, false, false);
    }
  }

  /**
   * Checks if the alert message is a test alert.
   *
   * @param alertInfo the Grafana alert message to check
   * @return true if the alert name is "TestAlert"
   */
  private boolean isTestAlert(GrafanaAlertMessage alertInfo) {
    return alertInfo.getCommonLabels().get("alertname").equals("TestAlert");
  }
}
