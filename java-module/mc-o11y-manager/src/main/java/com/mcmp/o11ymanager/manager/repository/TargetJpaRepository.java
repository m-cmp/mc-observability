package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.TargetEntity;
import com.mcmp.o11ymanager.manager.entity.TargetId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetJpaRepository extends JpaRepository<TargetEntity, TargetId> {

    List<TargetEntity> findByNsIdAndMciId(String nsId, String mciId);

    Optional<TargetEntity> findByNsIdAndMciIdAndTargetId(
            String nsId, String mciId, String targetId);

    Optional<TargetEntity> findTop1ByNsIdAndMciIdOrderByTargetIdAsc(String nsId, String mciId);

    Optional<TargetEntity> findByNsIdAndTargetId(String nsId, String targetId);
}
