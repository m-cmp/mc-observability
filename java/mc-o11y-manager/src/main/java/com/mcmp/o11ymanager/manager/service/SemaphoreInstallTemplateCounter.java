package com.mcmp.o11ymanager.manager.service;

import static com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Semaphore install template 슬롯(1..N) round-robin 카운터. {@link
 * com.mcmp.o11ymanager.manager.controller.BeylaController}와 OtelJavaController가 동일한 슬롯
 * 풀(install-agent_1..N)을 공유하므로 같은 카운터를 사용해야 한다 (둘 다 별도 인스턴스를 만들면 같은 template 슬롯에 부하 몰림 또는 충돌 가능).
 *
 * <p>주의: {@link com.mcmp.o11ymanager.manager.facade.AgentFacadeService}도 별개 카운터를 갖고 있어
 * Telegraf/FluentBit 흐름엔 적용 안 됨. 본 컴포넌트는 trace agent (Beyla, OTel Java) 전용.
 */
@Component
public class SemaphoreInstallTemplateCounter {

    private final Lock lock = new ReentrantLock();
    private int currentCount = 0;

    /** 다음 사용할 template 슬롯 번호(1..SEMAPHORE_MAX_PARALLEL_TASKS)를 round-robin으로 반환. */
    public int next() {
        try {
            lock.lock();
            if (currentCount >= SEMAPHORE_MAX_PARALLEL_TASKS) {
                currentCount = 0;
            }
            currentCount++;
            return currentCount;
        } finally {
            lock.unlock();
        }
    }
}
