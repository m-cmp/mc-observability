package com.mcmp.o11ymanager.global.aspect.request;

import com.mcmp.o11ymanager.global.annotation.AuthorizationHeader;
import com.mcmp.o11ymanager.infrastructure.util.JwtUtil;
import java.lang.annotation.Annotation;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class RequestIdAspect {

    private final RequestInfo requestInfo;

    @Around("execution(* com.mcmp.o11ymanager.controller..*.*(..)) && !within(com.mcmp.o11ymanager.oldController.HostWebSocketController)")
    public Object aroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        requestInfo.setRequestId(requestId);

        return joinPoint.proceed();


    }

    @Before("execution(* com.mcmp.o11ymanager.controller..*.*(..)) && !within(com.mcmp.o11ymanager.oldController.HostWebSocketController)")
    public void setRequestUserId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();

        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation.annotationType() == AuthorizationHeader.class) {
                    String token = (String) args[i];
                    if (token != null && !token.isEmpty()) {
                        String requestUserId = JwtUtil.getRequestUserId(token);
                        requestInfo.setRequestUserId(requestUserId);
                    }
                }
            }
        }
    }
}
