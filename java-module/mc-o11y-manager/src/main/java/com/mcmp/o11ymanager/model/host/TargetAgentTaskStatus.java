package com.mcmp.o11ymanager.model.host;

import lombok.Getter;

@Getter
public enum TargetAgentTaskStatus {
    PREPARING("Agent 관련 작업이 준비중 입니다."),
    INSTALLING("Agent 설치가 진행 중 입니다."),
    UPDATING("Agent 업데이트가 진행 중 입니다."),
    UPDATING_CONFIG("Agent 설정 업데이트가 진행 중 입니다."),
    UNINSTALLING("Agent 제거가 진행 중 입니다."),
    RESTARTING("Agent 재시작이 진행 중 입니다."),
    IDLE("현재 진행중인 에이전트 관련 작업이 없습니다.");

    private final String message;

    TargetAgentTaskStatus(String message) {
        this.message = message;
    }
}
