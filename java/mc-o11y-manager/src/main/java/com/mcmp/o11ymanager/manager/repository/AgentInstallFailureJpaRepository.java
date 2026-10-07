package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.AgentInstallFailureEntity;
import com.mcmp.o11ymanager.manager.entity.AgentInstallFailureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentInstallFailureJpaRepository
        extends JpaRepository<AgentInstallFailureEntity, AgentInstallFailureId> {

    List<AgentInstallFailureEntity> findByNsIdAndInfraId(String nsId, String infraId);
}
