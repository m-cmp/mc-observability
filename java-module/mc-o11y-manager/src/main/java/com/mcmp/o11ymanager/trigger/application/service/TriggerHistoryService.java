package com.mcmp.o11ymanager.trigger.application.service;


import com.mcmp.o11ymanager.trigger.application.common.exception.TriggerHistoryNotFoundException;
import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerHistory;
import com.mcmp.o11ymanager.trigger.application.persistence.repository.TriggerHistoryRepository;
import com.mcmp.o11ymanager.trigger.application.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for trigger history management.
 * Handles business logic for retrieving and updating trigger history records.
 */
@Service
@Transactional(readOnly = true)
public class TriggerHistoryService {

    private final TriggerHistoryRepository triggerHistoryRepository;

    public TriggerHistoryService(TriggerHistoryRepository triggerHistoryRepository) {
        this.triggerHistoryRepository = triggerHistoryRepository;
    }

    public CustomPageDto<TriggerHistoryDetailDto> getTriggerHistories(Pageable pageable) {
        Page<TriggerHistory> page = triggerHistoryRepository.findAll(pageable);
        return CustomPageDto.of(
                page, () -> page.getContent().stream().map(TriggerHistory::toDto).toList());
    }

    @Transactional
    public void updateComment(long id, TriggerHistoryCommentUpdateDto dto) {
        TriggerHistory triggerHistory =
                triggerHistoryRepository
                        .findById(id)
                        .orElseThrow(() -> new TriggerHistoryNotFoundException(id));
        triggerHistory.updateComment(dto.comment());
    }
}
