package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.K8sAgentTaskEntity;
import com.mcmp.o11ymanager.manager.entity.K8sAgentTaskId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface K8sAgentTaskJpaRepository
        extends JpaRepository<K8sAgentTaskEntity, K8sAgentTaskId> {

    Optional<K8sAgentTaskEntity> findByNsIdAndClusterIdAndNodeName(
            String nsId, String clusterId, String nodeName);

    List<K8sAgentTaskEntity> findByNsIdAndClusterId(String nsId, String clusterId);
}
