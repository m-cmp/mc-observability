package com.mcmp.o11ymanager.tracing;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.stereotype.Component;

@Component
public class ExecutorFactory {

  private static final int DEFAULT_POOL_SIZE = 10;

  private static final ExecutorService sharedExecutor =
      new ContextAwareExecutorService(Executors.newFixedThreadPool(DEFAULT_POOL_SIZE));

  public static ExecutorService getSharedExecutor() {
    return sharedExecutor;
  }


  @PreDestroy
  public void shutdown() {
    sharedExecutor.shutdown();
  }

  public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ContextAwareExecutorService(
        Executors.newFixedThreadPool(nThreads)
    );
  }

  public static ExecutorService newCachedThreadPool() {
    return new ContextAwareExecutorService(
        Executors.newCachedThreadPool()
    );
  }

  public static ScheduledExecutorService newScheduledThreadPool(int nThreads) {
    ScheduledExecutorService delegate = Executors.newScheduledThreadPool(nThreads);
    return new ContextAwareScheduledExecutorService(delegate);
  }


}
