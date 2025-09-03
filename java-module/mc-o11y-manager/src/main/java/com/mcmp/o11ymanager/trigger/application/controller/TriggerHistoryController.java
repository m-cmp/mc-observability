package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerHistoryCommentUpdateRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.TriggerHistoryPageResponse;
import com.mcmp.o11ymanager.trigger.application.service.TriggerHistoryService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryDetailDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for trigger history management Provides functionality for retrieving trigger
 * histories and updating comments.
 */
@RestController
@RequestMapping("/api/o11y/trigger/history")
public class TriggerHistoryController {

    private final TriggerHistoryService triggerHistoryService;

    /**
     * Constructor for TriggerHistoryController
     *
     * @param triggerHistoryService Service that handles trigger history related business logic
     */
    public TriggerHistoryController(TriggerHistoryService triggerHistoryService) {
        this.triggerHistoryService = triggerHistoryService;
    }

    /**
     * Retrieves trigger histories with pagination.
     *
     * @param page Page number (default: 1)
     * @param size Page size (default: 20)
     * @param sortBy Field to sort by (default: createdAt)
     * @param sortDirection Sort direction (default: desc)
     * @return Trigger history list with paging information
     */
    @GetMapping
    public ResponseEntity<TriggerHistoryPageResponse> getTriggerHistories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        CustomPageDto<TriggerHistoryDetailDto> dto =
                triggerHistoryService.getTriggerHistories(pageable);
        return ResponseEntity.ok(TriggerHistoryPageResponse.from(dto));
    }

    /**
     * Updates comment for the specified trigger history.
     *
     * @param id ID of the trigger history to update comment
     * @param request Comment update information
     * @return 200 OK response
     */
    // comment of deal with alert
    @PutMapping("/{id}/comment")
    public ResponseEntity<Void> updateComment(
            @PathVariable long id, @Valid @RequestBody TriggerHistoryCommentUpdateRequest request) {
        triggerHistoryService.updateComment(id, request.toDto());
        return ResponseEntity.ok().build();
    }
}
