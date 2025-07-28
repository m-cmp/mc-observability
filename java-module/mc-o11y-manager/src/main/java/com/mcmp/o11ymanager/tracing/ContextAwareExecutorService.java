//package com.mcmp.o11ymanager.tracing;
//
//import io.opentelemetry.context.Context;
//import io.opentelemetry.context.Scope;
//import java.util.List;
//import java.util.concurrent.AbstractExecutorService;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class ContextAwareExecutorService extends AbstractExecutorService {
//  private final ExecutorService delegate;
//
//  public ContextAwareExecutorService(ExecutorService delegate) {
//    this.delegate = delegate;
//  }
//
//  private Runnable wrap(Runnable task) {
//    Context ctx = Context.current();
//    return () -> {
//      try (Scope ignored = ctx.makeCurrent()) {
//        task.run();
//      }
//    };
//  }
//
//
//  private <T> Callable<T> wrap(Callable<T> task) {
//    Context ctx = Context.current();
//    return () -> {
//      try (Scope ignored = ctx.makeCurrent()) {
//        return task.call();
//      }
//    };
//  }
//
//  @Override
//  public void execute(Runnable command) {
//    delegate.execute(wrap(command));
//  }
//
//  /** AbstractExecutorService 추상 메서드 구현 **/
//  @Override
//  public void shutdown() {
//    delegate.shutdown();
//  }
//
//  @Override
//  public List<Runnable> shutdownNow() {
//    return delegate.shutdownNow();
//  }
//
//  @Override
//  public boolean isShutdown() {
//    return delegate.isShutdown();
//  }
//
//  @Override
//  public boolean isTerminated() {
//    return delegate.isTerminated();
//  }
//
//  @Override
//  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
//    return delegate.awaitTermination(timeout, unit);
//  }
//}
