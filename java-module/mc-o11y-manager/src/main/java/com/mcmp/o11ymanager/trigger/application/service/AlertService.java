package com.mcmp.o11ymanager.trigger.application.service;


import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.AlertServiceInternal;
import com.mcmp.o11ymanager.trigger.application.persistence.model.AlertTestHistory;
import com.mcmp.o11ymanager.trigger.application.persistence.repository.AlertTestHistoryRepository;
import com.mcmp.o11ymanager.trigger.application.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for alert test management.
 * Handles business logic for alert test history and validation.
 */
@Transactional
@Service
public class AlertService implements AlertServiceInternal {

    private final AlertTestHistoryRepository alertTestHistoryRepository;

    public AlertService(AlertTestHistoryRepository alertTestHistoryRepository) {
        this.alertTestHistoryRepository = alertTestHistoryRepository;
    }

    @Override
    public void createTestHistory(String message) {
        alertTestHistoryRepository.save(AlertTestHistory.create(message));
    }

    @Transactional(readOnly = true)
    public CustomPageDto<AlertTestHistoryDetailDto> getAlertTestHistories(Pageable pageable) {
        Page<AlertTestHistory> alertTestHistoryPage = alertTestHistoryRepository.findAll(pageable);
        return CustomPageDto.of(
                alertTestHistoryPage,
                () ->
                        alertTestHistoryPage.getContent().stream()
                                .map(AlertTestHistory::toDto)
                                .toList());
    }
}
