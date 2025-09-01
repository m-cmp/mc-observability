package com.mcmp.o11ymanager.trigger.adapter.internal.trigger;


import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;

public interface TriggerServiceInternal {
    void createTriggerHistory(AlertEvent alertEvent);

    ThresholdCondition getThresholdCondition(String title);
}
