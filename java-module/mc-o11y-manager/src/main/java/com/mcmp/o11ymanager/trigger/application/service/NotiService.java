package com.mcmp.o11ymanager.trigger.application.service;

import com.mcmp.o11ymanager.trigger.adapter.internal.notification.NotiServiceInternal;
import com.mcmp.o11ymanager.trigger.adapter.internal.notification.dto.NotiChannelCreateDto;
import com.mcmp.o11ymanager.trigger.application.common.exception.TriggerPolicyNotFoundException;
import com.mcmp.o11ymanager.trigger.application.persistence.model.*;
import com.mcmp.o11ymanager.trigger.application.persistence.repository.*;
import com.mcmp.o11ymanager.trigger.application.service.dto.*;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for notification management.
 * Handles business logic for notification channels, history, and message delivery.
 */
@Transactional
@Service
public class NotiService implements NotiServiceInternal {

    private final NotiHistoryRepository notiHistoryRepository;
    private final NotiChannelRepository notiChannelRepository;
    private final TriggerPolicyRepository triggerPolicyRepository;
    private final TriggerPolicyNotiChannelRepository triggerPolicyNotiChannelRepository;

    public NotiService(
            NotiHistoryRepository notiHistoryRepository,
            NotiChannelRepository notiChannelRepository,
            TriggerPolicyRepository triggerPolicyRepository,
            TriggerPolicyNotiChannelRepository triggerPolicyNotiChannelRepository) {
        this.notiHistoryRepository = notiHistoryRepository;
        this.notiChannelRepository = notiChannelRepository;
        this.triggerPolicyRepository = triggerPolicyRepository;
        this.triggerPolicyNotiChannelRepository = triggerPolicyNotiChannelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInitialized() {
        // 채널이 하나라도 있으면 초기화된 것으로 간주
        return notiChannelRepository.count() > 0;
    }


    @Override
    @Transactional
    public void initializeNotificationChannels(List<NotiChannelCreateDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<String> names = dtos.stream().map(NotiChannelCreateDto::name).toList();
        Set<String> existing = notiChannelRepository.findByNameIn(names).stream()
            .map(NotiChannel::getName)
            .collect(Collectors.toSet());

        List<NotiChannel> toInsert = dtos.stream()
            .filter(d -> !existing.contains(d.name()))
            .map(d -> NotiChannel.create(d.name(), d.type(), d.provider(), d.baseUrl(), d.isActive()))
            .toList();

        if (!toInsert.isEmpty()) {
            notiChannelRepository.saveAll(toInsert);
        }
    }


//    @Override
//    public void initializeNotificationChannels(List<NotiChannelCreateDto> dtos) {
//        List<NotiChannel> notificationChannels = NotiChannel.create(dtos);
//        notiChannelRepository.saveAll(notificationChannels);
//    }

    @Transactional(readOnly = true)
    @Override
    public List<TriggerPolicyNotiChannelDto> getNotiChannelsBy(String triggerPolicyTitle) {
        TriggerPolicy triggerPolicy =
                triggerPolicyRepository
                        .findByTitle(triggerPolicyTitle)
                        .orElseThrow(() -> new TriggerPolicyNotFoundException(triggerPolicyTitle));
        List<TriggerPolicyNotiChannel> triggerPolicyNotiChannels =
                triggerPolicyNotiChannelRepository.findByTriggerPolicy(triggerPolicy);
        return triggerPolicyNotiChannels.stream().map(TriggerPolicyNotiChannel::toDto).toList();
    }

    @Override
    public void createNotiHistory(List<NotiResult> results) {
        notiHistoryRepository.saveAll(NotiHistory.create(results));
    }

    @Transactional(readOnly = true)
    public List<NotiChannelDetailDto> getNotiChannels() {
        return notiChannelRepository.findAll().stream().map(NotiChannel::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CustomPageDto<NotiHistoryDetailDto> getNotiHistories(Pageable pageable) {
        Page<NotiHistory> notiHistoryPage = notiHistoryRepository.findAll(pageable);
        return CustomPageDto.of(
                notiHistoryPage,
                () -> notiHistoryPage.getContent().stream().map(NotiHistory::toDto).toList());
    }
}
