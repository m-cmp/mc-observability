package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerTargetDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerTargetDetailDto;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.Getter;

@Getter
@Table(name = "trigger_target")
@Entity
public class TriggerTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String uuid;

    private String namespaceId;

    private String targetScope;

    private String targetId;

    private boolean isActive;

    @JoinColumn(name = "trigger_policy_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TriggerPolicy triggerPolicy;

    @Transient private String key;

    public static TriggerTarget create(TriggerTargetDto dto) {
        TriggerTarget entity = new TriggerTarget();
        entity.uuid = UUID.randomUUID().toString();
        entity.namespaceId = dto.namespaceId();
        entity.targetScope = dto.targetScope();
        entity.targetId = dto.targetId();
        entity.isActive = dto.isActive();
        entity.setupKey();
        return entity;
    }

    public static TriggerTarget create(
            String namespaceId, String targetScope, String targetId, boolean isActive) {
        TriggerTarget entity = new TriggerTarget();
        entity.uuid = UUID.randomUUID().toString();
        entity.namespaceId = namespaceId;
        entity.targetScope = targetScope;
        entity.targetId = targetId;
        entity.isActive = isActive;
        entity.setupKey();
        return entity;
    }

    public TriggerTargetDetailDto toDto() {
        return TriggerTargetDetailDto.builder()
                .id(id)
                .uuid(uuid)
                .namespaceId(namespaceId)
                .targetScope(targetScope)
                .targetId(targetId)
                .isActive(isActive)
                .build();
    }

    public void setupKey() {
        key = namespaceId + "_" + targetScope + "-" + targetId;
    }

    public void setTriggerPolicy(TriggerPolicy triggerPolicy) {
        this.triggerPolicy = triggerPolicy;
    }

    public boolean isSameWith(TriggerTarget other) {
        this.setupKey();
        other.setupKey();
        return this.key.equals(other.getKey());
    }

    public void syncUuid(String uuid) {
        this.uuid = uuid;
    }
}
