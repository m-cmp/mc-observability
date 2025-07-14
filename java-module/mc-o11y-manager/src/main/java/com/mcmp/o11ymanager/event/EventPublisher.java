package com.mcmp.o11ymanager.event;

public interface EventPublisher {
    void publish(DomainEvent event);
} 