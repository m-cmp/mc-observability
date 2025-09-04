package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;

public class AgentConfigNotFoundException extends BaseException {
    public AgentConfigNotFoundException(String requestId, String hostId, Agent agent) {
        super(
                requestId,
                ErrorCode.AGENT_CONFIG_NOT_FOUND,
                String.format("호스트(ID: %s)에 %s 에이전트 설정이 존재하지 않습니다.", hostId, agent.getName()));
    }
}
