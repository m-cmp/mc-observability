package com.mcmp.o11ymanager.event.listner;

import com.mcmp.o11ymanager.event.HostUpdateNotifyMultipleEvent;
import com.mcmp.o11ymanager.event.HostUpdateNotifySingleEvent;
import com.mcmp.o11ymanager.service.interfaces.HostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HostUpdateNotifyListener {

  private final HostService hostService;
  private final SimpMessagingTemplate messagingTemplate;

  @EventListener
  public void handleSingleEvent(HostUpdateNotifySingleEvent evt) {
    log.info("HostUpdateNotifySingleEvent received");
    messagingTemplate.convertAndSend("/topic/host/" + evt.getHostId(), hostService.findById(evt.getHostId()));
  }

  @EventListener
  public void handleMultipleEvent(HostUpdateNotifyMultipleEvent evt) {
    log.info("HostUpdateNotifyMultipleEvent received");
    messagingTemplate.convertAndSend("/topic/hosts", hostService.list());
  }
}
