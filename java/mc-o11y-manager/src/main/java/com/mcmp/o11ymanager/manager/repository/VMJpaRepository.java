package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.entity.VmId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VMJpaRepository extends JpaRepository<VMEntity, VmId> {

    List<VMEntity> findByNsIdAndMciId(String nsId, String mciId);

    Optional<VMEntity> findByNsIdAndMciIdAndVmId(String nsId, String mciId, String vmId);

    Optional<VMEntity> findTop1ByNsIdAndMciIdOrderByVmIdAsc(String nsId, String mciId);

    Optional<VMEntity> findByNsIdAndVmId(String nsId, String vmId);
}
