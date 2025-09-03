package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerPolicy;
import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerTarget;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggerTargetRepository extends JpaRepository<TriggerTarget, Long> {

    List<TriggerTarget> findByTriggerPolicy(TriggerPolicy triggerPolicy);
}
