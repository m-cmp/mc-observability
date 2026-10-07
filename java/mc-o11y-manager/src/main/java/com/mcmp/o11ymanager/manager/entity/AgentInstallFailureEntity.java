package com.mcmp.o11ymanager.manager.entity;

import com.mcmp.o11ymanager.manager.enums.Agent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Why the last install of an agent on a VM node failed. Installs run in the background, so the
 * reason can no longer be returned by the install request. It lives in its own table rather than
 * the {@code node} table because the install can fail before the node is registered (e.g. no
 * reachable InfluxDB), when there is no node row to put it on. Removed when the agent is installed
 * or uninstalled again.
 */
@Entity
@Table(name = "vm_agent_install_failure")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@IdClass(AgentInstallFailureId.class)
public class AgentInstallFailureEntity {

    @Id private String nsId;

    @Id private String infraId;

    @Id private String nodeId;

    @Id
    @Enumerated(EnumType.STRING)
    private Agent agent;

    @Column(length = 2000)
    private String reason;

    private LocalDateTime failedAt;
}
