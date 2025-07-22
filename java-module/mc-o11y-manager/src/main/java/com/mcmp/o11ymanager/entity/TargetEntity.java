package com.mcmp.o11ymanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
@EntityListeners(AuditingEntityListener.class)
public class TargetEntity {
  public static final String HOST_TYPE_HOST = "target";
  public static final String HOST_TYPE_VM = "vm";

  @Id
  private String id;

  @Column(nullable = false)
  private String name;

  private String aliasName;

  private String description;

  @Column(nullable = false)
  private String csp;

  @Enumerated(EnumType.STRING)
  private TargetStatus targetStatus;

  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus logTaskStatus;


  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus monitoringTaskStatus;

  private String targetMonitoringAgentTaskId;

  private String targetLogAgentTaskId;

  private String monitoringServiceStatus;

  private String logServiceStatus;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  private String nsId;

  private String mciId;

  private String subGroup;

  private String state;

}