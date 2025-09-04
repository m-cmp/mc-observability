package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.service.dto.NotiHistoryDetailDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;

@Getter
@Table(name = "noti_history")
@Entity
public class NotiHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String channel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recipients;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exception;

    private boolean isSucceeded;

    public static List<NotiHistory> create(List<NotiResult> results) {
        return results.stream()
                .map(
                        result -> {
                            NotiHistory notiHistory = new NotiHistory();
                            notiHistory.channel = result.getChannel();
                            notiHistory.recipients = result.getRecipients();
                            if (result.getException() != null) {
                                notiHistory.exception = result.getException();
                            }
                            notiHistory.isSucceeded = result.isSucceeded();
                            return notiHistory;
                        })
                .toList();
    }

    public NotiHistoryDetailDto toDto() {
        return NotiHistoryDetailDto.builder()
                .id(id)
                .channel(channel)
                .recipients(Arrays.stream(recipients.split(", ")).toList())
                .exception(exception)
                .isSucceeded(isSucceeded)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
