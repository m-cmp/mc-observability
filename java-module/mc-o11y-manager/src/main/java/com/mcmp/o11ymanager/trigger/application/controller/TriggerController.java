package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerPolicyCreateRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerPolicyNotiChannelUpdateRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerVMAddRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerVMRemoveRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.TriggerPolicyPageResponse;
import com.mcmp.o11ymanager.trigger.application.service.TriggerService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelUpdateDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for trigger policy management Provides functionality for creating, deleting,
 * retrieving trigger policies, updating notification channels, and adding/removing vms.
 */
@RestController
@RequestMapping("/api/o11y/trigger/policy")
public class TriggerController {

    private final TriggerService triggerService;

    /**
     * Constructor for TriggerController
     *
     * @param triggerService Service that handles trigger policy related business logic
     */
    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    /**
     * Creates a new trigger policy.
     *
     * @param request Request object containing information needed to create trigger policy
     * @return 201 Created response with the created trigger policy ID
     */
    @PostMapping
    public ResponseEntity<?> createTriggerPolicy(
            @Valid @RequestBody TriggerPolicyCreateRequest request) {
        long triggerPolicyId = triggerService.createTriggerPolicy(request.toDto());
        return ResponseEntity.created(URI.create("/api/o11y/trigger/policy/" + triggerPolicyId))
                .build();
    }

    /**
     * Deletes the trigger policy with the specified ID.
     *
     * @param id ID of the trigger policy to delete
     * @return 202 Accepted response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTriggerPolicy(@PathVariable long id) {
        triggerService.deleteTriggerPolicy(id);
        return ResponseEntity.accepted().build();
    }

    /**
     * Retrieves trigger policy list based on paging and sorting conditions.
     *
     * @param page Page number (default: 1)
     * @param size Page size (default: 10)
     * @param sortBy Field to sort by (default: id)
     * @param direction Sort direction (default: desc)
     * @return Trigger policy list with paging information
     */
    @GetMapping
    public ResponseEntity<?> getTriggerPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortBy));
        CustomPageDto<TriggerPolicyDetailDto> triggerPolicies =
                triggerService.getTriggerPolicies(pageable);
        return ResponseEntity.ok(TriggerPolicyPageResponse.from(triggerPolicies));
    }

    /**
     * Updates notification channels for the specified trigger policy.
     *
     * @param id ID of the trigger policy to update
     * @param request List of notification channel update information
     * @return 202 Accepted response
     */
    @PutMapping("/{id}/channel")
    public ResponseEntity<?> updateTriggerChannelByName(
            @PathVariable long id,
            @Valid @RequestBody List<TriggerPolicyNotiChannelUpdateRequest> request) {
        List<TriggerPolicyNotiChannelUpdateDto> dtos =
                request.stream().map(TriggerPolicyNotiChannelUpdateRequest::toDto).toList();
        triggerService.updateTriggerPolicyNotiChannelByName(id, dtos);
        return ResponseEntity.accepted().build();
    }

    /**
     * Adds a new trigger vm to the specified trigger policy.
     *
     * @param id ID of the trigger policy to add vm to
     * @param request Information of the trigger vm to add
     * @return 202 Accepted response
     */
    @PostMapping("/{id}/vm")
    public ResponseEntity<?> addTriggerVM(
            @PathVariable long id, @Valid @RequestBody TriggerVMAddRequest request) {
        triggerService.addTriggerVM(id, request.toDto());
        return ResponseEntity.accepted().build();
    }

    // Patch (isActive) 추가

    /**
     * Removes a trigger vm from the specified trigger policy.
     *
     * @param id ID of the trigger policy to remove vm from
     * @param request Information of the trigger vm to remove
     * @return 202 Accepted response
     */
    @DeleteMapping("/{id}/vm")
    public ResponseEntity<?> removeTriggerVM(
            @PathVariable long id, @Valid @RequestBody TriggerVMRemoveRequest request) {
        triggerService.removeTriggerVM(id, request.toDto());
        return ResponseEntity.accepted().build();
    }
}
