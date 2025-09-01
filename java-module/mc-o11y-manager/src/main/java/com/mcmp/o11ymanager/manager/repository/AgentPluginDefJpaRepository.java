package com.mcmp.o11ymanager.manager.repository;

import com.mcmp.o11ymanager.manager.entity.AgentPluginDefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentPluginDefJpaRepository extends JpaRepository<AgentPluginDefEntity, Long> {

    @Modifying
    @Query("DELETE FROM AgentPluginDefEntity")
    void deleteAll();
}