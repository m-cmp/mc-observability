package com.mcmp.o11ymanager.repository;

import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentPluginDefJpaRepository extends JpaRepository<AgentPluginDefEntity, Long> {

    @Modifying
    @Query("DELETE FROM AgentPluginDefEntity")
    void deleteAll();

    boolean existsByNameAndPluginType(String name, String pluginType);
}