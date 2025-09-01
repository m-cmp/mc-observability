package com.mcmp.o11ymanager.manager.model.host;

import lombok.Getter;

@Getter
public enum TargetStatus {
    RUNNING("호스트가 동작 중입니다."),
    FAILED("호스트에 연결할 수 없습니다.");

    private final String message;

    TargetStatus(String message) {
        this.message = message;
    }
}
