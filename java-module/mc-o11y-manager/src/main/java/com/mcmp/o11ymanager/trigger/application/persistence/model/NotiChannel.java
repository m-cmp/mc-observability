package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.adapter.internal.notification.dto.NotiChannelCreateDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;

@Getter
@Table(name = "noti_channel")
@Entity
public class NotiChannel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String name;

    private String type;

    private String provider;

    private String baseUrl;

    private boolean isActive;

    public static List<NotiChannel> create(List<NotiChannelCreateDto> dtos) {
        return dtos.stream()
                .map(
                        dto -> {
                            NotiChannel entity = new NotiChannel();
                            entity.name = dto.name();
                            entity.type = dto.type();
                            entity.provider = dto.provider();
                            entity.baseUrl = dto.baseUrl();
                            entity.isActive = dto.isActive();
                            return entity;
                        })
                .toList();
    }

    public static NotiChannel create(
            String name, String type, String provider, String baseUrl, boolean isActive) {
        NotiChannel entity = new NotiChannel();
        entity.name = name;
        entity.type = type;
        entity.provider = provider;
        entity.baseUrl = baseUrl;
        entity.isActive = isActive;
        return entity;
    }

    public NotiChannelDetailDto toDto() {
        return NotiChannelDetailDto.builder()
                .id(id)
                .name(name)
                .type(type)
                .provider(provider)
                .baseUrl(baseUrl)
                .isActive(isActive)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
