package com.mcmp.o11ymanager.trigger.infrastructure;

import com.mcmp.o11ymanager.trigger.adapter.internal.notification.NotiServiceInternal;
import com.mcmp.o11ymanager.trigger.adapter.internal.notification.dto.NotiChannelCreateDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Application initialization class Handles initialization tasks that need to be performed after the
 * application starts up, specifically initializing notification channels from configured
 * properties.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationInitializer {

    private final NotiFactory notiFactory;
    private final NotiServiceInternal notificationService;
    private final ApplicationContext ctx;


    @EventListener(ApplicationReadyEvent.class)
    public void initNotification() {
        // DevTools/Cloud NamedContext 등으로 두 번 이상 실행되는 것 방지
        if (ctx.getParent() != null) {
            return;
        }

        // 이미 초기화 돼 있으면 스킵
        if (notificationService.isInitialized()) {
            log.info("[NOTI-INIT] already initialized -> skip");
            return;
        }

        var dtos = notiFactory.getNotiChannelProps().stream()
            .map(NotiChannelCreateDto::from)
            .toList();

        notificationService.initializeNotificationChannels(dtos);
        log.info("[NOTI-INIT] initialized {} channels", dtos.size());
    }



    /**
     * Initializes notification channels when the application is ready. This method is triggered
     * after the application has fully started and is ready to serve requests. It retrieves
     * notification channel properties from the factory and initializes them in the service.
     */
//    @EventListener(ApplicationReadyEvent.class)
//    public void initNotification() {
//        notificationService.initializeNotificationChannels(
//                notiFactory.getNotiChannelProps().stream()
//                        .map(NotiChannelCreateDto::from)
//                        .toList());
//    }
}
