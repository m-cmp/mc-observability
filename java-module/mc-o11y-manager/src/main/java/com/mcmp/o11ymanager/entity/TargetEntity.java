package com.mcmp.o11ymanager.entity;

import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "target")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
@IdClass(TargetId.class)
@EntityListeners(AuditingEntityListener.class)
public class TargetEntity {
  public static final String HOST_TYPE_HOST = "target";
  public static final String HOST_TYPE_VM = "vm";

  @Id
  private String nsId;

  @Id
  private String mciId;

  @Id
  private String targetId;

  @ManyToOne
  @JoinColumn(name = "influxdb_id")
  private InfluxEntity influxDb;

  private Long influxSeq;

  @Column(nullable = false)
  private String name;

  private String description;

  @Enumerated(EnumType.STRING)
  private TargetStatus targetStatus;

  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus monitoringAgentTaskStatus;
  
  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus logAgentTaskStatus;

  private String targetMonitoringAgentTaskId;

  private String targetLogAgentTaskId;

  private AgentServiceStatus monitoringServiceStatus;

  private AgentServiceStatus logServiceStatus;

}