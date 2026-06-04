package com.mcmp.o11ymanager.manager.global.aspect.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope // HTTP 요청 하나당 새 인스턴스 생성
@Getter
@Setter
@NoArgsConstructor
public class RequestInfo {
    private String requestId;
}

// 값은 RequestIdAspect 클래스에서 받는다 자세한 것은 해당 클래스 주석 참고
