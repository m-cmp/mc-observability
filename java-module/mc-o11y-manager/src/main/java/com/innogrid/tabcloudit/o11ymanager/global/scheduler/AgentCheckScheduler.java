package com.innogrid.tabcloudit.o11ymanager.global.scheduler;

import com.innogrid.tabcloudit.o11ymanager.dto.host.HostResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.facade.HostFacadeService;
import com.innogrid.tabcloudit.o11ymanager.service.HostService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Order
public class AgentCheckScheduler {

  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private static final int AGENT_CHECK_THREAD_MAX = 10;
  private final HostService hostService;
  private final HostFacadeService hostFacadeService;
  private ExecutorService executor;

  @Scheduled(fixedRateString = "${health.check-interval:5000}")
  public void run() {
    if (!isHostCheckSchedulerAlive()) {
      startHostCheckScheduler();
    }
  }

  @PreDestroy
  public void stopHostCheckScheduler() {
    if (executor != null && !executor.isShutdown()) {
      executor.shutdownNow();
      log.debug("Agent check scheduler stopped");
    }
    isRunning.set(false);
  }

  public void startHostCheckScheduler() {
    if (!isRunning.get()) {
      log.debug("Starting agent check scheduler");
      isRunning.set(true);
      log.debug("▶️ Agent check scheduler started!");
      checkAgents();
    }
  }

  public boolean isHostCheckSchedulerAlive() {
    return isRunning.get();
  }

  public void checkAgents() {
    try {
      List<HostResponseDTO> hostResponseDTOList = hostFacadeService.list();
      log.debug("Will check agents for {} hosts.", hostResponseDTOList.size());

      if (hostResponseDTOList.isEmpty()) {
        log.debug("Host list is empty. No need to check agents for hosts.");
        return;
      }

      executor = Executors.newFixedThreadPool(AGENT_CHECK_THREAD_MAX);

      for (final HostResponseDTO hostResponseDTO : hostResponseDTOList) {
        executor.submit(() -> {
          try {
            hostFacadeService.processHostCheck(hostResponseDTO);
          } catch (Exception ex) {
            isRunning.set(false);
          }
        });
      }

      executor.shutdown();
      boolean finished = executor.awaitTermination(60, TimeUnit.SECONDS);

      if (!finished) {
        log.warn("⏱ Agent check timed out. Forcing shutdown...");
        executor.shutdownNow();
      } else {
        log.debug("🏁 Agent check for all hosts finished!");
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("⚠️ Agent check interrupted: {}", e.getMessage());
    } catch (Exception e) {
      log.error("[AgentCheckScheduler ERROR] Agent check failed for all hosts.", e);
    } finally {
      isRunning.set(false);
    }
  }


//  public void checkAgents() {
//    try {
//      List<HostResponseDTO> hostResponseDTOList = hostFacadeService.list();
//      log.debug("Will check agents for {} hosts.", hostResponseDTOList.size());
//
//      if (hostResponseDTOList.isEmpty()) {
//        log.debug("Host list is empty. No need to check agents for hosts.");
//        isRunning.set(false);
//        return;
//      }
//
//      //여기서부터 null
//      executor = Executors.newFixedThreadPool(AGENT_CHECK_THREAD_MAX);
//      for (final HostResponseDTO hostResponseDTO : hostResponseDTOList) {
//        executor.submit(() -> hostFacadeService.processHostCheck(hostResponseDTO));
//      }
//
//
//      log.debug("Agent check for all hosts finished!");
////    }
////    } catch (InterruptedException e) {
////      Thread.currentThread().interrupt();
////      log.debug("Agent check stopped: {}", e.getMessage());
////
////
//    } catch (Exception e) {
//      log.error("[AgentCheckScheduler] Agent check failed for all hosts.", e);
//    }
////    finally {
////      isRunning.set(false);
////    }
//  }
}
