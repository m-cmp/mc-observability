package com.mcmp.o11ymanager.manager.global.runner;

import com.mcmp.o11ymanager.manager.service.AgentPluginDefServiceImpl;
import com.mcmp.o11ymanager.manager.service.SemaphoreService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One-time startup preparation (agent plugin definitions, Semaphore project/templates, agent task
 * status reset).
 *
 * <p>Runs on a background daemon thread so it does NOT block application startup. Semaphore
 * initialization in particular calls the Semaphore REST API and used to take several minutes,
 * during which the manager (running this in {@code setApplicationContext}, before the web server
 * started) didn't accept any requests. The app now starts immediately and these resources become
 * ready shortly after — only agent install / metric-config depends on them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreparationRunner implements ApplicationRunner {

    private final SemaphoreService semaphoreService;
    private final AgentPluginDefServiceImpl agentPluginDefServiceImpl;
    private final VMService vmService;

    @Override
    public void run(ApplicationArguments args) {
        Thread t = new Thread(this::prepare, "manager-preparation");
        t.setDaemon(true);
        t.start();
    }

    private void prepare() {
        try {
            agentPluginDefServiceImpl.initializePluginDefinitions();
        } catch (Exception e) {
            log.error("Failed to initialize agent plugin definitions", e);
        }

        try {
            log.info("Starting semaphore initialization (background).");
            semaphoreService.initSemaphore();
            log.info("Semaphore initialization completed successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize semaphore.\n{}", e.getMessage(), e);
        }

        log.info("Initializing agent task statuses for all hosts.");
        try {
            vmService.resetAllHostAgentTaskStatus();
        } catch (Exception e) {
            log.error("Failed to reset agent task statuses", e);
        }
        log.info("Agent task status initialization for all hosts completed.");
    }
}
