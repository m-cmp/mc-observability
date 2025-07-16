package com.mcmp.o11ymanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "host",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ip", "port"}))
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

  @OneToOne(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private AccessInfoEntity credential;

  @Column(nullable = false)
  private String name;

  private String aliasName;

  private String description;

  @Column(nullable = false)
  private String csp;

  @Enumerated(EnumType.STRING)
  private TargetStatus targetStatus;

  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus targetAgentTaskStatus;

  private String targetMonitoringAgentTaskId;

  private String targetLogAgentTaskId;

  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus monitoringServiceStatus;

  @Enumerated(EnumType.STRING)
  private TargetAgentTaskStatus logServiceStatus;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  private String nsId;

  private String mciId;

  private String vmId;

  private String subGroup;

  private String state;

}