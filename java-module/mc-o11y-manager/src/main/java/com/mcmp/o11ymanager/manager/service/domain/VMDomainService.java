package com.mcmp.o11ymanager.manager.service.domain;

import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.mapper.host.VMMapper;
import com.mcmp.o11ymanager.manager.repository.VMJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VMDomainService {

    private final VMMapper vmMapper;
    private final VMNotificationService vmNotificationService;
    private final VMJpaRepository vmJpaRepository;

    @Transactional
    public void updateVM(VMEntity vmEntity) {
        VMEntity savedVM = vmJpaRepository.save(vmEntity);

        vmNotificationService.notifyVMUpdate(
                savedVM.getNsId(), savedVM.getMciId(), savedVM.getVmId(), vmMapper.toDTO(savedVM));
        vmNotificationService.notifyAllVMsUpdate(vmJpaRepository.findAll());
        log.info("VMDomainService : {}", savedVM);
    }
}
