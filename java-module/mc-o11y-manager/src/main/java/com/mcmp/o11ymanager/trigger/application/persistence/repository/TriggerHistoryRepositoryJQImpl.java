package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.TriggerHistory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TriggerHistoryRepositoryJQImpl implements TriggerHistoryRepositoryJQ {

    private final DSLContext dslContext;

    @Override
    public boolean existsTriggerHistories(List<TriggerHistory> triggerHistories) {
        if (triggerHistories == null || triggerHistories.isEmpty()) {
            return false;
        }

        List<Condition> conditions =
                triggerHistories.stream()
                        .map(
                                triggerHistory ->
                                        DSL.field("trigger_title")
                                                .eq(triggerHistory.getTriggerTitle())
                                                .and(
                                                        DSL.field("resource_type")
                                                                .eq(
                                                                        triggerHistory
                                                                                .getResourceType()))
                                                .and(
                                                        DSL.field("namespace_id")
                                                                .eq(
                                                                        triggerHistory
                                                                                .getNamespaceId()))
                                                .and(
                                                        DSL.field("mci_id")
                                                                .eq(triggerHistory.getMciId()))
                                                .and(
                                                        DSL.field("target_id")
                                                                .eq(triggerHistory.getTargetId()))
                                                .and(
                                                        DSL.field("starts_at")
                                                                .eq(triggerHistory.getStartsAt())))
                        .toList();

        return dslContext.fetchExists(DSL.table("trigger_history"), DSL.or(conditions));
    }
}
