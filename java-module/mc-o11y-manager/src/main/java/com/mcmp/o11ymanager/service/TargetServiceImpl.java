package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.entity.AccessInfoEntity;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.event.HostUpdateNotifyMultipleEvent;
import com.mcmp.o11ymanager.event.HostUpdateNotifySingleEvent;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class TargetServiceImpl implements TargetService {

  private final TargetJpaRepository targetJpaRepository;
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;


  @Override
  public TargetDTO get(String nsId, String mciId, String targetId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciIdTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", nsId + "/" + mciId));
    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(entity);
  }


  @Override
  public TargetDTO getByNsMci(String nsId, String mciId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", nsId + "/" + mciId));
    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(entity);
  }


  @Override
  public List<TargetDTO> list() {
    return targetJpaRepository.findAll().stream().map(com.mcmp.o11ymanager.dto.target.TargetDTO::fromEntity).toList();
  }

  @Override
  public TargetDTO post(String nsId, String mciId, String targetId, TargetRegisterDTO dto) {

    String encryptedSshKey = null;

    try {
      if (dto.getAccessInfo() != null &&
          dto.getAccessInfo().getSshKey() != null &&
          !dto.getAccessInfo().getSshKey().isBlank()) {

        encryptedSshKey = ChaCha20Poly3105Util.encryptString(dto.getAccessInfo().getSshKey());
      }
    } catch (Exception e) {
      throw new RuntimeException("SSH Key encryption failed", e);
    }

    TargetEntity target = TargetEntity.builder()
        .id(targetId)
        .name(dto.getName())
        .aliasName(dto.getAliasName())
        .description(dto.getDescription())
        .csp(dto.getCsp())
        .nsId(nsId)
        .mciId(mciId)
        .subGroup(dto.getSubGroup())
        .monitoringServiceStatus(TargetAgentTaskStatus.IDLE)
        .logServiceStatus(TargetAgentTaskStatus.IDLE)
        .build();

    AccessInfoEntity accessInfoEntity = AccessInfoEntity.builder()
        .id(targetId)
        .ip(dto.getAccessInfo().getIp())
        .port(dto.getAccessInfo().getPort())
        .user(dto.getAccessInfo().getUser())
        .sshKey(encryptedSshKey)
        .target(target)
        .build();

    target.setCredential(accessInfoEntity);

    TargetEntity saved = targetJpaRepository.save(target);

    event.publishEvent(
        HostUpdateNotifyMultipleEvent.builder().build()
    );

    return TargetDTO.fromEntity(saved);
  }


  @Override
  public TargetDTO put(String targetId, String nsId, String mciId, TargetUpdateDTO request) {
    TargetEntity target = targetJpaRepository.findById(targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));

    if (!nsId.equals(target.getNsId()) || !mciId.equals(target.getMciId())) {
      throw new IllegalArgumentException("failed to update target");
    }

    if (request.getName() != null) {
      target.setName(request.getName());
    }

    if (request.getAliasName() != null) {
      target.setAliasName(request.getAliasName());
    }

    if (request.getDescription() != null) {
      target.setDescription(request.getDescription());
    }

    TargetEntity updated = targetJpaRepository.save(target);

    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(updated.getId())
            .build()
    );

    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(updated);
  }


  @Override
  @Transactional
  public void delete(String targetId, String nsId, String mciId) {
    TargetEntity entity = targetJpaRepository.findById(targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));

    if (!nsId.equals(entity.getNsId()) || !mciId.equals(entity.getMciId())) {
      throw new IllegalArgumentException("nsId 또는 mciId가 일치하지 않습니다.");
    }

    targetJpaRepository.deleteById(targetId);
  }
}
















