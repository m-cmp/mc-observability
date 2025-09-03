package com.mcmp.o11ymanager.manager.model.tcp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PingRequest {
    private String ip;
    private int port;
}
