package com.innogrid.tabcloudit.o11ymanager.repository;

import com.innogrid.tabcloudit.o11ymanager.entity.HistoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryJpaRepository extends JpaRepository<HistoryEntity, String> {

  List<HistoryEntity> findByHostId(@NonNull String hostId);


}
