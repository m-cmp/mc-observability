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
            "execution(* com.mcmp.o11ymanager.manager.controller.*.*(..)) && !within(com.mcmp.o11ymanager.manager.controller.VMWebSocketController)")
    public Object aroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        requestInfo.setRequestId(requestId);

        return joinPoint.proceed();
    }
}

/*
* 1. HTTP 요청이 controller 패키지의 아무 컨트롤러 메서드에 진입
* 2. Spring이 RequestInfo 빈을 현재 요청용으로 새로 생성 (최초 접근 시점)
* 3. RequestIdAspect가 컨트롤러 메서드 호출을 가로 채서 UUID 생성 -> requestInfo.setRequestId(...)
* 4. constoller 매서드 실행 -> 그 아래 BeylaFacadeService 등에서 같은 requestInfo 프롲시 꺼내면 방금 세팅된 UUID가 들어있음
* 5. 요청이 끝나면 해당 REquestInfo 인스턴스는 폐기
* */