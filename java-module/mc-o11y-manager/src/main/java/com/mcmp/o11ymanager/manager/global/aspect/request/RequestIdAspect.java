package com.mcmp.o11ymanager.manager.global.aspect.request;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class RequestIdAspect {

    private final RequestInfo requestInfo;

    @Around("execution(* com.mcmp.o11ymanager.controller..*.*(..)) && !within(com.mcmp.o11ymanager.manager.controller.TargetWebSocketController)")
    public Object aroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        requestInfo.setRequestId(requestId);

        return joinPoint.proceed();


    }

}
