package com.mcmp.o11ymanager.repository;

import com.mcmp.o11ymanager.entity.TargetEntity;
import jakarta.persistence.LockModeType;
import java.lang.annotation.Target;
import java.util.Optional;

import javax.swing.text.html.Option;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetJpaRepository extends JpaRepository<TargetEntity, String> {

  @NotNull List<TargetEntity> findAll();

  @Query("SELECT t FROM TargetEntity t WHERE t.nsId = :nsId AND t.mciId = :mciId")
  List<TargetEntity> findByNsIdAndMciId(String nsId, String mciId);

  @Query("SELECT t FROM TargetEntity t WHERE t.nsId = :nsId AND t.mciId = :mciId AND t.targetId = :targetId")
  Optional<TargetEntity> findByNsIdAndMciIdAndTargetId(String nsId, String mciId, String targetId);

  @Query("""
      select t.influxSeq
      from TargetEntity t
      where t.nsId = :nsId and t.mciId = :mciId
      """)
  Optional<Integer> findInfluxSeqByNsIdAndMciId(String nsId, String mciId);
}

