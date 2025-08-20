package com.mcmp.o11ymanager.repository;

import com.mcmp.o11ymanager.entity.InfluxEntity;
import com.mcmp.o11ymanager.entity.TargetEntity;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InfluxJpaRepository extends JpaRepository<InfluxEntity, Long> {

}

