package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.NodeId;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VMJpaRepository extends JpaRepository<VMEntity, NodeId> {

    List<VMEntity> findByNsIdAndInfraId(String nsId, String infraId);

    Optional<VMEntity> findByNsIdAndInfraIdAndNodeId(String nsId, String infraId, String nodeId);

    Optional<VMEntity> findTop1ByNsIdAndInfraIdOrderByNodeIdAsc(String nsId, String infraId);

    Optional<VMEntity> findByNsIdAndNodeId(String nsId, String nodeId);
}
