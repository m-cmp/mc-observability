package com.innogrid.tabcloudit.o11ymanager.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredOn();
    String getEventType();
} 