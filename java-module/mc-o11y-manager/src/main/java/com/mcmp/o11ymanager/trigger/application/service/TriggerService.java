package com.mcmp.o11ymanager.trigger.application.service;

import com.mcmp.o11ymanager.trigger.adapter.external.alert.*;
import com.mcmp.o11ymanager.trigger.adapter.external.alert.dto.*;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.ManagerPort;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.TriggerServiceInternal;
import com.mcmp.o11ymanager.trigger.application.common.dto.*;
import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.common.exception.TriggerPolicyNotFoundException;
import com.mcmp.o11ymanager.trigger.application.persistence.model.*;
import com.mcmp.o11ymanager.trigger.application.persistence.repository.*;
import com.mcmp.o11ymanager.trigger.application.service.dto.*;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for trigger policy management. Handles business logic for creating, deleting, and
 * managing trigger policies and their associated vms and notification channels.
 */
@Transactional
@Service
public class TriggerService implements TriggerServiceInternal {

    private final TriggerHistoryRepository triggerHistoryRepository;
    private final TriggerPolicyRepository triggerPolicyRepository;
    private final TriggerPolicyNotiChannelRepository triggerPolicyNotiChannelRepository;
    private final NotiChannelRepository notiChannelRepository;
    private final AlertManager alertManager;
    private final ManagerPort managerPort;

    public void addTriggerVM(long id, TriggerVMDto triggerVMDto) {

        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findById(id)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(id));

        String datasourceUid =
                managerPort.getInfluxUid(
                        triggerVMDto.namespaceId(),
                        triggerVMDto.targetScope(),
                        triggerVMDto.targetId());

        TriggerVM triggerVM = TriggerVM.create(triggerVMDto);
        boolean isAdded = triggerPolicy.addIfNotContains(triggerVM);
        if (isAdded) {
            triggerPolicyRepository.save(triggerPolicy);
            alertManager.createAlertRule(
                    AlertRuleCreateDto.from(triggerPolicy.toDto(), triggerVM.toDto()),
                    datasourceUid);
        }
    }

    public TriggerService(
            TriggerHistoryRepository triggerHistoryRepository,
            TriggerPolicyRepository triggerPolicyRepository,
            TriggerPolicyNotiChannelRepository triggerPolicyNotiChannelRepository,
            NotiChannelRepository notiChannelRepository,
            ManagerPort managerPort,
            AlertManager alertManager) {
        this.triggerHistoryRepository = triggerHistoryRepository;
        this.triggerPolicyRepository = triggerPolicyRepository;
        this.triggerPolicyNotiChannelRepository = triggerPolicyNotiChannelRepository;
        this.notiChannelRepository = notiChannelRepository;
        this.managerPort = managerPort;
        this.alertManager = alertManager;
    }

    public long createTriggerPolicy(TriggerPolicyCreateDto triggerPolicyCreateDto) {
        TriggerPolicy triggerPolicy = TriggerPolicy.create(triggerPolicyCreateDto);
        triggerPolicy = triggerPolicyRepository.save(triggerPolicy);
        return triggerPolicy.getId();
    }

    public void deleteTriggerPolicy(long id) {
        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findById(id)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(id));
        List<TriggerVM> triggerVMs = triggerPolicy.getTriggerVMs();
        for (TriggerVM triggerVM : triggerVMs) {
            alertManager.deleteAlertRule(triggerVM.getUuid());
        }
        triggerPolicyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CustomPageDto<TriggerPolicyDetailDto> getTriggerPolicies(Pageable pageable) {
        Page<TriggerPolicy> triggerPolicyPage = triggerPolicyRepository.findAll(pageable);
        return CustomPageDto.of(
                triggerPolicyPage,
                () -> triggerPolicyPage.getContent().stream().map(TriggerPolicy::toDto).toList());
    }

    @Transactional
    public void updateTriggerPolicyNotiChannelByName(
            long id, List<TriggerPolicyNotiChannelUpdateDto> dtos) {

        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findById(id)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(id));

        Map<String, String> channelNameMap =
                Map.of(
                        "sms", "sms_naver-cloud",
                        "email", "email_smtp.gmail.com",
                        "kakao", "kakao_naver-cloud",
                        "slack", "slack");

        Map<String, String> channelRecipientMap = new HashMap<>();

        List<String> notiChannelNames =
                dtos.stream()
                        .map(
                                dto -> {
                                    String simpleName = dto.channelName().toLowerCase();
                                    String dbName =
                                            channelNameMap.getOrDefault(simpleName, simpleName);

                                    if (!channelNameMap.containsKey(simpleName)) {
                                        throw new InvalidNotificationTypeException(
                                                "Unsupported notification channel: "
                                                        + simpleName
                                                        + " (allowed values: kakao, sms, email, slack)");
                                    }

                                    String recipients = String.join(", ", dto.recipients());
                                    channelRecipientMap.put(dbName, recipients);
                                    return dbName;
                                })
                        .toList();

        List<NotiChannel> notiChannels = notiChannelRepository.findByNameIn(notiChannelNames);

        List<TriggerPolicyNotiChannel> triggerPolicyNotiChannels =
                TriggerPolicyNotiChannel.create(triggerPolicy, notiChannels, channelRecipientMap);

        triggerPolicyNotiChannelRepository.saveAll(triggerPolicyNotiChannels);
    }

    public void removeTriggerVM(long id, TriggerVMDto triggerVMDto) {
        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findById(id)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(id));
        TriggerVM triggerVM = TriggerVM.create(triggerVMDto);
        boolean isRemoved = triggerPolicy.removeIfContains(triggerVM);
        if (isRemoved) {
            triggerPolicyRepository.save(triggerPolicy);
            alertManager.deleteAlertRule(triggerVM.getUuid());
        }
    }

    @Override
    public void createTriggerHistory(AlertEvent alertEvent) {
        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findByTitle(alertEvent.getTitle())
                        .orElseThrow(
                                () -> new TriggerPolicyNotFoundException(alertEvent.getTitle()));
        List<TriggerHistory> triggerHistories = TriggerHistory.create(triggerPolicy, alertEvent);
        if (!triggerHistoryRepository.existsTriggerHistories(triggerHistories)) {
            triggerHistoryRepository.saveAll(triggerHistories);
        }
    }

    @Override
    public ThresholdCondition getThresholdCondition(String title) {
        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findByTitle(title)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(title));
        return ThresholdCondition.from(triggerPolicy.getThresholdCondition());
    }
}
