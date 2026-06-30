package com.mcmp.o11ymanager.manager.entity;

import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persisted agent-task state for a single Kubernetes node. K8s node metric/log presence is
 * otherwise derived live (cb-spider + InfluxDB/Loki), which has no notion of "an install is
 * currently running" — so two browser tabs both see an enabled Install button and can race. This
 * row gives the install/uninstall flow a durable, cross-tab status
 * (INSTALLING/UNINSTALLING/FINISHED/...), mirroring how VM nodes track it in the {@code node}
 * table.
 */
@Entity
@Table(name = "k8s_agent_task")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
@IdClass(K8sAgentTaskId.class)
public class K8sAgentTaskEntity {

    @Id private String nsId;

    @Id private String clusterId;

    @Id private String nodeName;

    /** Telegraf (monitoring) agent task state. */
    @Enumerated(EnumType.STRING)
    private VMAgentTaskStatus monitoringTaskStatus;

    /** Fluent Bit (log) agent task state. */
    @Enumerated(EnumType.STRING)
    private VMAgentTaskStatus logTaskStatus;
}
