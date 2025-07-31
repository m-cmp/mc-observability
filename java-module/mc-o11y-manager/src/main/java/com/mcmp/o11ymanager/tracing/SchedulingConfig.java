package com.mcmp.o11ymanager.tracing;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

  private final ScheduledExecutorService contextAwareScheduledExecutorService;

  public SchedulingConfig(ScheduledExecutorService contextAwareScheduledExecutorService) {
    this.contextAwareScheduledExecutorService = contextAwareScheduledExecutorService;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.setScheduler(contextAwareScheduledExecutorService);
  }
}
