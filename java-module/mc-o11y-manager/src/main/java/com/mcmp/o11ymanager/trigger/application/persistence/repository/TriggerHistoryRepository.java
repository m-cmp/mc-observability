package com.mcmp.o11ymanager.trigger.application.persistence.repository;


import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggerHistoryRepository
        extends JpaRepository<TriggerHistory, Long>, TriggerHistoryRepositoryJQ {}
