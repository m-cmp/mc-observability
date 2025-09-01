package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryDetailDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent.AlertDetail;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@Table(name = "trigger_history")
@Entity
public class TriggerHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String triggerTitle;

    private String aggregationType;

    private String holdDuration;

    private String repeatInterval;

    private String resourceType;

    private String namespaceId;

    private String mciId;

    private String targetId;

    private String threshold;

    private String resourceUsage;

    private String alertLevel;

    private String status;

    private String comment;

    private LocalDateTime startsAt;

    public static List<TriggerHistory> create(TriggerPolicy triggerPolicy, AlertEvent alertEvent) {
        List<TriggerHistory> triggerHistories = new ArrayList<>();
        triggerHistories.addAll(
                alertEvent.getInfoAlerts().stream()
                        .map(alertDetail -> createWith(triggerPolicy, alertDetail))
                        .toList());
        triggerHistories.addAll(
                alertEvent.getWarningAlerts().stream()
                        .map(alertDetail -> createWith(triggerPolicy, alertDetail))
                        .toList());
        triggerHistories.addAll(
                alertEvent.getCriticalAlerts().stream()
                        .map(alertDetail -> createWith(triggerPolicy, alertDetail))
                        .toList());
        return triggerHistories;
    }

    private static TriggerHistory createWith(TriggerPolicy triggerPolicy, AlertDetail alertDetail) {
        TriggerHistory triggerHistory = new TriggerHistory();
        triggerHistory.triggerTitle = triggerPolicy.getTitle();
        triggerHistory.aggregationType = triggerPolicy.getAggregationType();
        triggerHistory.holdDuration = triggerPolicy.getHoldDuration();
        triggerHistory.repeatInterval = triggerPolicy.getRepeatInterval();
        triggerHistory.resourceType = triggerPolicy.getResourceType();
        triggerHistory.namespaceId = alertDetail.getNamespaceId();
        triggerHistory.mciId = alertDetail.getMciId();
        triggerHistory.targetId = alertDetail.getTargetId();
        triggerHistory.threshold = alertDetail.getThreshold();
        triggerHistory.resourceUsage = alertDetail.getResourceUsage();
        triggerHistory.alertLevel = alertDetail.getAlertLevel();
        triggerHistory.status = alertDetail.getStatus();
        triggerHistory.startsAt = OffsetDateTime.parse(alertDetail.getStartsAt()).toLocalDateTime();
        return triggerHistory;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    public TriggerHistoryDetailDto toDto() {
        return TriggerHistoryDetailDto
                .builder()
                .id(id)
                .triggerTitle(triggerTitle)
                .aggregationType(aggregationType)
                .holdDuration(holdDuration)
                .repeatInterval(repeatInterval)
                .resourceType(resourceType)
                .namespaceId(namespaceId)
                .mciId(mciId)
                .targetId(targetId)
                .threshold(threshold)
                .resourceUsage(resourceUsage)
                .alertLevel(alertLevel)
                .status(status)
                .comment(comment)
                .startsAt(startsAt)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
