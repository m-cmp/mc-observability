package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.entity.AgentInstallFailureEntity;
import com.mcmp.o11ymanager.manager.entity.AgentInstallFailureId;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.repository.AgentInstallFailureJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records why a VM agent install failed, see {@link AgentInstallFailureEntity}. */
@Service
@RequiredArgsConstructor
public class AgentInstallFailureService {

    private static final int MAX_REASON_LENGTH = 2000;

    private final AgentInstallFailureJpaRepository repository;

    // Own transaction: a failure is recorded after the install transaction has already rolled
    // back or committed, and must stay even if a surrounding one rolls back.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String nsId, String infraId, String nodeId, Agent agent, String reason) {
        String text = reason == null || reason.isBlank() ? "Unknown error" : reason;
        if (text.length() > MAX_REASON_LENGTH) {
            text = text.substring(0, MAX_REASON_LENGTH);
        }
        repository.save(
                AgentInstallFailureEntity.builder()
                        .nsId(nsId)
                        .infraId(infraId)
                        .nodeId(nodeId)
                        .agent(agent)
                        .reason(text)
                        .failedAt(LocalDateTime.now())
                        .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String nsId, String infraId, String nodeId, Agent agent) {
        AgentInstallFailureId id = new AgentInstallFailureId(nsId, infraId, nodeId, agent);
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> find(String nsId, String infraId, String nodeId, Agent agent) {
        return repository
                .findById(new AgentInstallFailureId(nsId, infraId, nodeId, agent))
                .map(AgentInstallFailureEntity::getReason);
    }

    @Transactional(readOnly = true)
    public List<AgentInstallFailureEntity> findByNsInfra(String nsId, String infraId) {
        return repository.findByNsIdAndInfraId(nsId, infraId);
    }
}
