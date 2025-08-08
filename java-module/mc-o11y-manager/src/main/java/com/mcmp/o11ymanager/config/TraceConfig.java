package com.mcmp.o11ymanager.config;

import com.mcmp.o11ymanager.tracing.ContextAwareExecutorService;
import com.mcmp.o11ymanager.tracing.ContextAwareScheduledExecutorService;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;


import java.util.concurrent.*;
import org.springframework.core.Ordered;

@Configuration
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "otel.enabled", havingValue = "true", matchIfMissing = true)
class TraceConfig {

  @Bean(name="o11yExecutor", destroyMethod = "shutdown")
  @Primary
  public ExecutorService contextAwareExecutorService() {
    ExecutorService delegate = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    return new ContextAwareExecutorService(delegate);
  }

  @Bean(name = "o11yScheduler", destroyMethod = "shutdown")
  public ScheduledExecutorService contextAwareScheduledExecutorService() {
    ScheduledExecutorService delegate = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    return new ContextAwareScheduledExecutorService(delegate);
  }


}