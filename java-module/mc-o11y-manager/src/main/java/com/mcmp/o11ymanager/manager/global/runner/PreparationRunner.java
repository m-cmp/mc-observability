package com.mcmp.o11ymanager.manager.global.runner;

import com.mcmp.o11ymanager.manager.service.AgentPluginDefServiceImpl;
import com.mcmp.o11ymanager.manager.service.SemaphoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreparationRunner implements ApplicationContextAware {

    private final SemaphoreService semaphoreService;
    private final AgentPluginDefServiceImpl agentPluginDefServiceImpl;

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext)
            throws BeansException {
        try {
            agentPluginDefServiceImpl.initializePluginDefinitions();
        } catch (Exception e) {
            log.error("Failed to initialize agent plugin definitions", e);
        }

        try {
            log.info("Starting semaphore initialization.");
            semaphoreService.initSemaphore();
            log.info("Semaphore initialization completed successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize semaphore.\n{}", e.getMessage(), e);
        }

        log.info("Initializing agent task statuses for all hosts.");
        // vmService.resetAllHostAgentTaskStatus();
        log.info("Agent task status initialization for all hosts completed.");
    }
}
