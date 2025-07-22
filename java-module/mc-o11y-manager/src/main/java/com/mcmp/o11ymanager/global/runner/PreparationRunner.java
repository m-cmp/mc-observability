package com.mcmp.o11ymanager.global.runner;

import com.mcmp.o11ymanager.oldService.domain.HostService;
import com.mcmp.o11ymanager.oldService.domain.OldSemaphoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.mcmp.o11ymanager.infrastructure.util.ChaCha20Poly3105Util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreparationRunner implements ApplicationContextAware {

  private final OldSemaphoreService oldSemaphoreService;
  private final HostService hostService;

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext)
      throws BeansException {
    try {
      File keyFile = new File(KEY_FILE_PATH);
      if (!keyFile.exists()) {
        log.info("키 파일이 존재하지 않습니다. 새로 생성합니다. 🚀");
        generateKeyFile();
        log.info("키 파일 생성 완료: " + KEY_FILE_PATH + " 🎉");
      }
    } catch (Exception e) {
      log.error("키 파일 확인이 실패 하였습니다. 😵💫\n {}", e.getMessage(), e);
    }

    try {
      log.info("Semaphore 초기화를 시작 합니다. 🚀");
      oldSemaphoreService.initSemaphore();
      log.info("Semaphore 초기화가 완료 되었습니다. 🎉");
    } catch (Exception e) {
      log.error("Semaphore 초기화를 실패 하였습니다. 😵💫\n {}", e.getMessage(), e);
    }

    log.info("호스트들의 에이전트 Task 상태를 초기화 하고 있습니다. 🚀");
    hostService.resetAllHostAgentTaskStatus();
    log.info("호스트들의 에이전트 Task 상태 초기화가 완료 되었습니다. 🎉");
  }
}
