package com.mcmp.o11ymanager.trigger.application.persistence.model;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
@Getter
@Table(name = "trigger_policy_noti_channel")
@Entity
public class TriggerPolicyNotiChannel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "trigger_policy_id")
    private TriggerPolicy triggerPolicy;

    @ManyToOne
    @JoinColumn(name = "noti_channel_id")
    private NotiChannel notiChannel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recipients;

    public static List<TriggerPolicyNotiChannel> create(
            TriggerPolicy triggerPolicy,
            List<NotiChannel> notiChannels,
            Map<String, String> channelRecipientMap) {
        return notiChannels.stream()
                .map(
                        notiChannel -> {
                            TriggerPolicyNotiChannel triggerPolicyNotiChannel =
                                    new TriggerPolicyNotiChannel();
                            triggerPolicyNotiChannel.triggerPolicy = triggerPolicy;
                            triggerPolicyNotiChannel.notiChannel = notiChannel;
                            triggerPolicyNotiChannel.recipients =
                                    channelRecipientMap.get(notiChannel.getName());
                            return triggerPolicyNotiChannel;
                        })
                .toList();
    }

    public TriggerPolicyNotiChannelDto toDto() {
        if (recipients == null || recipients.isBlank()) {
            throw new InvalidNotificationTypeException(
                    "Recipients are required for notification channel: " + notiChannel.getName());
        }

        return TriggerPolicyNotiChannelDto.builder()
                .id(this.notiChannel.getId())
                .name(this.notiChannel.getName())
                .type(this.notiChannel.getType())
                .provider(this.notiChannel.getProvider())
                .baseUrl(this.notiChannel.getBaseUrl())
                .recipients(Arrays.stream(recipients.split(", ")).toList())
                .isActive(this.notiChannel.isActive())
                .build();
    }
}
