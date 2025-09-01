package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfluxJpaRepository extends JpaRepository<InfluxEntity, Long> {

}

