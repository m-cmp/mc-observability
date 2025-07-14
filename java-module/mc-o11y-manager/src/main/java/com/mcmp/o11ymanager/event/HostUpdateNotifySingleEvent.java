package com.mcmp.o11ymanager.event;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class HostUpdateNotifySingleEvent extends BaseDomainEvent{
    private final String hostId;
}
