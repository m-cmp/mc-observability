package com.mcmp.o11ymanager.trigger.adapter.internal.notification;

import com.mcmp.o11ymanager.trigger.adapter.internal.notification.dto.NotiChannelCreateDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import java.util.List;



public interface NotiServiceInternal {


    void initializeNotificationChannels(List<NotiChannelCreateDto> dtos);
    boolean isInitialized();

    List<TriggerPolicyNotiChannelDto> getNotiChannelsBy(String triggerPolicyTitle);

    void createNotiHistory(List<NotiResult> result);
}
