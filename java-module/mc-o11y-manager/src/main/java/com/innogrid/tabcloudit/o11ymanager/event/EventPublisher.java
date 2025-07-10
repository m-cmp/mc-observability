package com.innogrid.tabcloudit.o11ymanager.event;

public interface EventPublisher {
    void publish(DomainEvent event);
} 