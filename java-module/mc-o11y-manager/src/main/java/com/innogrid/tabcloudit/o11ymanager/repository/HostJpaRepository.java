package com.innogrid.tabcloudit.o11ymanager.repository;

import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostJpaRepository extends JpaRepository<HostEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM HostEntity h WHERE h.id = :id")
    boolean existsById(@NonNull String id);

    List<HostEntity> findByIpAndPort(String ip, int port);

}
