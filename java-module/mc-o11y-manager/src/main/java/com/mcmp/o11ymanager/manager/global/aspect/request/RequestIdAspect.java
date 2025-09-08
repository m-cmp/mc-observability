package com.mcmp.o11ymanager.manager.global.aspect.request;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RequestIdAspect {

    private final RequestInfo requestInfo;

    @Around(
            "execution(* com.mcmp.o11ymanager.controller..*.*(..)) && !within(com.mcmp.o11ymanager.manager.controller.VMWebSocketController)")
    public Object aroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        requestInfo.setRequestId(requestId);

        return joinPoint.proceed();
    }
}
