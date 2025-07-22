package com.mcmp.o11ymanager.repository;

import com.mcmp.o11ymanager.entity.TargetEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetJpaRepository extends JpaRepository<TargetEntity, String> {

  boolean existsById(@NonNull String id);

  Optional<TargetEntity> findById(String id);

  List<TargetEntity> findAll();

  Optional<TargetEntity> findByNsIdAndMciId(String nsId, String mciId);

  @Query("SELECT t FROM TargetEntity t WHERE t.nsId = :nsId AND t.mciId = :mciId AND t.id = :targetId")
  Optional<TargetEntity> findByNsIdAndMciIdTargetId(String nsId, String mciId, String targetId);


}

