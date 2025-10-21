package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.service.dto.AlertTestHistoryDetailDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "alert_test_history")
@Entity
public class AlertTestHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    public static AlertTestHistory create(String message) {
        AlertTestHistory entity = new AlertTestHistory();
        entity.message = message;
        return entity;
    }

    public AlertTestHistoryDetailDto toDto() {
        return AlertTestHistoryDetailDto.builder()
                .id(id)
                .message(message)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
