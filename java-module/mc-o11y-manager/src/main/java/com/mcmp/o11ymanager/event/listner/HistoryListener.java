package com.mcmp.o11ymanager.event.listner;

import com.mcmp.o11ymanager.entity.HistoryEntity;
import com.mcmp.o11ymanager.event.AgentHistoryEvent;
import com.mcmp.o11ymanager.event.AgentHistoryFailEvent;
import com.mcmp.o11ymanager.service.interfaces.HistoryService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryListener {

  private final HistoryService historyService;


  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleSuccessHistoryEvent(AgentHistoryEvent evt) {
    HistoryEntity historyEntity = new HistoryEntity();
    historyEntity.setHostId(evt.getHostId());
    historyEntity.setTimestamp(LocalDateTime.now());
    historyEntity.setAgentAction(evt.getAgentAction());
    historyEntity.setRequestUserId(evt.getRequestUserId());
    historyEntity.setSuccess(true);
    historyEntity.setReason(evt.getReason());

    historyService.save(historyEntity);
  }

  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleFailureHistoryEvent(AgentHistoryFailEvent evt) {
    HistoryEntity historyEntity = new HistoryEntity();
    historyEntity.setHostId(evt.getHostId());
    historyEntity.setTimestamp(LocalDateTime.now());
    historyEntity.setAgentAction(evt.getAgentAction());
    historyEntity.setRequestUserId(evt.getRequestUserId());
    historyEntity.setSuccess(false);
    historyEntity.setReason(evt.getReason());

    historyService.save(historyEntity);
  }
}
