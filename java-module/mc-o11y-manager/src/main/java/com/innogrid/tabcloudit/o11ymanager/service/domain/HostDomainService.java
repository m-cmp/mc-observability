package com.innogrid.tabcloudit.o11ymanager.service.domain;

import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.exception.host.DuplicatedHostIpException;
import com.innogrid.tabcloudit.o11ymanager.mapper.host.HostMapper;
import com.innogrid.tabcloudit.o11ymanager.repository.HostCustomRepo;
import com.innogrid.tabcloudit.o11ymanager.repository.HostJpaRepository;
import com.innogrid.tabcloudit.o11ymanager.global.error.ResourceNotExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class HostDomainService {

  private final HostMapper hostMapper;
  private final HostCustomRepo hostCustomRepo;

  private final HostNotificationService hostNotificationService;

  private final HostJpaRepository hostJpaRepository;

  @Transactional(readOnly = true)
  public List<HostEntity> listHost() {
    return hostJpaRepository.findAll();
  }

  @Transactional(readOnly = true)
  public HostEntity getHostById(String requestId, String id) {
    if (id == null) {
      return null;
    }
    return hostJpaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestId, "HostEntity", id
        ));
  }



  @Transactional
  public HostEntity updateHost(String requestId, HostEntity hostEntity) {
    HostEntity savedHost = hostCustomRepo.save(requestId, hostEntity, true);

    hostNotificationService.notifyHostUpdate(savedHost.getId(), hostMapper.toDTO(savedHost));
    hostNotificationService.notifyAllHostsUpdate(hostJpaRepository.findAll());
    log.info("[HostDomainService : {}", savedHost);
    return savedHost;
  }



}
