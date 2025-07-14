package com.mcmp.o11ymanager.entity;

import com.mcmp.o11ymanager.enums.AgentAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Setter
@Table(name = "history")
public class HistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    //여기에 host 서비스 실패 예외 시 해당 host의 id값이 저장되어야함
    private String hostId;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgentAction agentAction;

    private String requestUserId;

    private boolean isSuccess;

    @Column(name="reason", length=1024)
    private String reason;
}
