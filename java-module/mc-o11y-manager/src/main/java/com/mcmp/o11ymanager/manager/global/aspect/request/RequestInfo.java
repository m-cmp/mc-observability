package com.mcmp.o11ymanager.manager.global.aspect.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Getter
@Setter
@NoArgsConstructor
public class RequestInfo {
    private String requestId;
}
