package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriggerPolicyRepository extends JpaRepository<TriggerPolicy, Long> {

    Optional<TriggerPolicy> findByTitle(String triggerPolicyTitle);
}
