package com.mcmp.o11ymanager.manager.entity;

import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "vm")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
@IdClass(VmId.class)
@EntityListeners(AuditingEntityListener.class)
public class VMEntity {

    @Id private String nsId;

    @Id private String mciId;

    @Id private String vmId;

    @Column(name = "influx_id", nullable = false)
    private Long influxSeq;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private VMStatus vmStatus;

    @Enumerated(EnumType.STRING)
    private VMAgentTaskStatus monitoringAgentTaskStatus;

    @Enumerated(EnumType.STRING)
    private VMAgentTaskStatus logAgentTaskStatus;

    private String vmMonitoringAgentTaskId;

    private String vmLogAgentTaskId;

    private AgentServiceStatus monitoringServiceStatus;

    private AgentServiceStatus logServiceStatus;

    @Enumerated(EnumType.STRING)
    private AgentStatus monitoringAgentStatus;

    @Enumerated(EnumType.STRING)
    private AgentStatus logAgentStatus;
}
