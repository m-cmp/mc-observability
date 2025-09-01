package com.mcmp.o11ymanager.manager.service.domain;

import com.mcmp.o11ymanager.manager.entity.TargetEntity;
import com.mcmp.o11ymanager.manager.mapper.host.TargetMapper;
import com.mcmp.o11ymanager.manager.repository.TargetJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class TargetDomainService {

  private final TargetMapper targetMapper;
  private final TargetNotificationService targetNotificationService;
  private final TargetJpaRepository targetJpaRepository;

  @Transactional
  public void updateTarget(TargetEntity targetEntity) {
    TargetEntity savedTarget = targetJpaRepository.save(targetEntity);

    targetNotificationService.notifyTargetUpdate(savedTarget.getNsId(), savedTarget.getMciId(), savedTarget.getTargetId(),
            targetMapper.toDTO(savedTarget));
    targetNotificationService.notifyAllTargetsUpdate(targetJpaRepository.findAll());
    log.info("TargetDomainService : {}", savedTarget);
  }
}
