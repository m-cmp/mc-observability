package com.mcmp.o11ymanager.trigger.infrastructure.external.notification;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import java.util.List;


public interface NotiFactory {
  /**
   * Gets all configured notification channel properties.
   *
   * @return list of notification channel properties
   */
  List<NotiProperty> getNotiChannelProps();

  /**
   * Creates a notification object based on channel configuration and alert event.
   *
   * @param notiChannelDto notification channel configuration
   * @param alertEvent alert event information
   * @return created notification object
   */
  Noti createNoti(TriggerPolicyNotiChannelDto notiChannelDto, AlertEvent alertEvent);
}
