package com.mcmp.o11ymanager.tracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorFactory {
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
