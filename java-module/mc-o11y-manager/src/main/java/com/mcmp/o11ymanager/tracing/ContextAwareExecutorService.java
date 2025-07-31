package com.mcmp.o11ymanager.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ContextAwareExecutorService extends AbstractExecutorService {
  private final ExecutorService delegate;

  public ContextAwareExecutorService(ExecutorService delegate) {
    this.delegate = delegate;
  }

  protected Runnable wrap(Runnable task) {
    Context ctx = Context.current();
    return () -> {
      try (Scope ignored = ctx.makeCurrent()) {
        task.run();
      }
    };
  }

  protected <T> Callable<T> wrap(Callable<T> task) {
    Context ctx = Context.current();
    return () -> {
      try (Scope ignored = ctx.makeCurrent()) {
        return task.call();
      }
    };
  }

  private <T> Collection<? extends Callable<T>> wrapAll(Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(this::wrap).toList();
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(wrap(command));
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(wrap(task), result);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(wrapAll(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return delegate.invokeAll(wrapAll(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return delegate.invokeAny(wrapAll(tasks));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(wrapAll(tasks), timeout, unit);
  }
}
