package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerPolicy;
import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerVM;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggerVMRepository extends JpaRepository<TriggerVM, Long> {

    List<TriggerVM> findByTriggerPolicy(TriggerPolicy triggerPolicy);
}
