package com.mcmp.o11ymanager.trigger.application.persistence.repository;


import com.mcmp.o11ymanager.trigger.application.persistence.model.NotiHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotiHistoryRepository extends JpaRepository<NotiHistory, Long> {}
