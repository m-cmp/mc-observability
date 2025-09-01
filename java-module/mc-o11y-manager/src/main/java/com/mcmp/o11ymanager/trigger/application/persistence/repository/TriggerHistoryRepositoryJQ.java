package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerHistory;
import java.util.List;


public interface TriggerHistoryRepositoryJQ {

    boolean existsTriggerHistories(List<TriggerHistory> triggerHistories);
}
