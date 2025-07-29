package com.mcmp.o11ymanager.global.runner;

import com.mcmp.o11ymanager.service.SemaphoreService;
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

  private final SemaphoreService oldSemaphoreService;

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext)
      throws BeansException {
    try {
      log.info("Semaphore 초기화를 시작 합니다. 🚀");
      oldSemaphoreService.initSemaphore();
      log.info("Semaphore 초기화가 완료 되었습니다. 🎉");
    } catch (Exception e) {
      log.error("Semaphore 초기화를 실패 하였습니다. 😵💫\n {}", e.getMessage(), e);
    }

    log.info("호스트들의 에이전트 Task 상태를 초기화 하고 있습니다. 🚀");
//    targetService.resetAllHostAgentTaskStatus();
    log.info("호스트들의 에이전트 Task 상태 초기화가 완료 되었습니다. 🎉");
  }
}
