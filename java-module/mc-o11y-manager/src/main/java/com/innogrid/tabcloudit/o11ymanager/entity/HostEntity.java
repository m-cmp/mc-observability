package com.innogrid.tabcloudit.o11ymanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "host",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ip", "port"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
public class HostEntity {
    public static final String HOST_TYPE_HOST = "host";
    public static final String HOST_TYPE_VM = "vm";

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String hostname;

    private String description;

    @Column(nullable = false)
    private String credential_id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String cloud_service;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String user;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private HostStatus host_status;

    @Enumerated(EnumType.STRING)
    private HostAgentTaskStatus host_monitoring_agent_task_status;

    @Enumerated(EnumType.STRING)
    private HostAgentTaskStatus host_log_agent_task_status;

    private String host_monitoring_agent_task_id;

    private String host_log_agent_task_id;

    private String monitoringServiceStatus;

    private String logServiceStatus;

    private String monitoring_agent_config_git_hash;

    private String log_agent_config_git_hash;

    private String monitoring_agent_version;

    private String log_agent_version;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
