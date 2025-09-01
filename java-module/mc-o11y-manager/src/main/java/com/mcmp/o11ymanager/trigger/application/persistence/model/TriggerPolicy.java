package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyCreateDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerTargetDetailDto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@Table(name = "trigger_policy")
@Entity
public class TriggerPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String title;

    private String description;

    private String thresholdCondition;

    private String resourceType;

    private String aggregationType;

    private String holdDuration;

    private String repeatInterval;

    @OneToMany(
            mappedBy = "triggerPolicy",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private List<TriggerTarget> triggerTargets = new ArrayList<>();

    @OneToMany(
            mappedBy = "triggerPolicy",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private List<TriggerPolicyNotiChannel> triggerPolicyNotiChannels = new ArrayList<>();

    public static TriggerPolicy create(TriggerPolicyCreateDto dto) {
        TriggerPolicy entity = new TriggerPolicy();
        entity.title = dto.title();
        entity.description = dto.description();
        entity.thresholdCondition = dto.thresholdCondition().toJson();
        entity.resourceType = dto.resourceType().getMeasurement();
        entity.aggregationType = dto.aggregationType().getName();
        entity.holdDuration = dto.holdDuration();
        entity.repeatInterval = dto.repeatInterval();
        return entity;
    }

    public static TriggerPolicy create(
            String title,
            String description,
            String thresholdCondition,
            String resourceType,
            String aggregationType,
            String holdDuration,
            String repeatInterval) {
        TriggerPolicy entity = new TriggerPolicy();
        entity.title = title;
        entity.description = description;
        entity.thresholdCondition = thresholdCondition;
        entity.resourceType = resourceType;
        entity.aggregationType = aggregationType;
        entity.holdDuration = holdDuration;
        entity.repeatInterval = repeatInterval;
        return entity;
    }

    public TriggerPolicyDetailDto toDto() {
        List<TriggerTargetDetailDto> targets =
                triggerTargets.stream().map(TriggerTarget::toDto).toList();
        List<TriggerPolicyNotiChannelDto> notiChannels =
                triggerPolicyNotiChannels.stream().map(TriggerPolicyNotiChannel::toDto).toList();

        return TriggerPolicyDetailDto.builder()
                .id(id)
                .title(title)
                .description(description)
                .thresholdCondition(ThresholdCondition.from(thresholdCondition))
                .resourceType(resourceType)
                .aggregationType(aggregationType)
                .holdDuration(holdDuration)
                .repeatInterval(repeatInterval)
                .targets(targets)
                .notiChannels(notiChannels)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }

    public boolean addIfNotContains(TriggerTarget triggerTarget) {
        boolean hasNotTarget = this.triggerTargets.stream().noneMatch(triggerTarget::isSameWith);
        if (hasNotTarget) {
            triggerTarget.setTriggerPolicy(this);
            triggerTargets.add(triggerTarget);
        }
        return hasNotTarget;
    }

    public boolean removeIfContains(TriggerTarget triggerTarget) {
        return this.triggerTargets.removeIf(
                target -> {
                    if (target.isSameWith(triggerTarget)) {
                        triggerTarget.syncUuid(target.getUuid());
                        return true;
                    }
                    return false;
                });
    }
}
