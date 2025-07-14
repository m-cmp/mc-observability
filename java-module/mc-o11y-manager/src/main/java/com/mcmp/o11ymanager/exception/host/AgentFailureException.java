package com.mcmp.o11ymanager.exception.host;

import com.mcmp.o11ymanager.event.AgentHistoryEvent;
import com.mcmp.o11ymanager.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

@Getter
public class AgentFailureException extends BaseException {
    public AgentFailureException(AgentHistoryEvent failureEvent, ApplicationEventPublisher eventPublisher) {
        super(failureEvent.getRequestId(), ErrorCode.AGENT_FAILURE,  failureEvent.getReason());

        if (eventPublisher != null) {
            eventPublisher.publishEvent(failureEvent);
        }
    }
}
