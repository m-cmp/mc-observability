package com.mcmp.o11ymanager.trigger.application.persistence.repository;


import com.mcmp.o11ymanager.trigger.application.persistence.model.AlertTestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertTestHistoryRepository extends JpaRepository<AlertTestHistory, Long> {}
