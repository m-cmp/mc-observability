package com.innogrid.tabcloudit.o11ymanager.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent implements DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;
    
    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
} 