package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.manager.global.vm.ResBody;
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
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for trigger policy management Provides functionality for creating, deleting,
 * retrieving trigger policies, updating notification channels, and adding/removing vms.
 */
@RestController
@RequestMapping("/api/o11y/trigger/policy")
public class TriggerController {

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResBody<Map<String, Long>> createTriggerPolicy(
            @Valid @RequestBody TriggerPolicyCreateRequest request) {

        long triggerPolicyId = triggerService.createTriggerPolicy(request.toDto());
        Map<String, Long> responseData = Map.of("id", triggerPolicyId);
        return new ResBody<>(responseData);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResBody<Void> deleteTriggerPolicy(@PathVariable long id) {
        triggerService.deleteTriggerPolicy(id);
        return new ResBody<>();
    }

    @GetMapping
    public ResBody<TriggerPolicyPageResponse> getTriggerPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortBy));

        CustomPageDto<TriggerPolicyDetailDto> triggerPolicies =
                triggerService.getTriggerPolicies(pageable);

        return new ResBody<>(TriggerPolicyPageResponse.from(triggerPolicies));
    }

    @PutMapping("/{id}/channel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResBody<Void> updateTriggerChannelByName(
            @PathVariable long id,
            @Valid @RequestBody List<TriggerPolicyNotiChannelUpdateRequest> request) {

        List<TriggerPolicyNotiChannelUpdateDto> dtos =
                request.stream().map(TriggerPolicyNotiChannelUpdateRequest::toDto).toList();
        triggerService.updateTriggerPolicyNotiChannelByName(id, dtos);
        return new ResBody<>();
    }

    @PostMapping("/{id}/vm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResBody<Void> addTriggerVM(
            @PathVariable long id, @Valid @RequestBody TriggerVMAddRequest request) {
        triggerService.addTriggerVM(id, request.toDto());
        return new ResBody<>();
    }

    @DeleteMapping("/{id}/vm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResBody<Void> removeTriggerVM(
            @PathVariable long id, @Valid @RequestBody TriggerVMRemoveRequest request) {
        triggerService.removeTriggerVM(id, request.toDto());
        return new ResBody<>();
    }
}
