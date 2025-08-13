package com.mcmp.o11ymanager.exception.log;

import com.mcmp.o11ymanager.exception.host.BaseException;
import com.mcmp.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

/**
 * Loki 쿼리 시간 범위가 서버 제한을 초과했을 때 발생하는 예외
 */
@Getter
public class LokiTimeRangeExceededException extends BaseException {
    
    private final String queryLength;
    private final String limit;
    
    public LokiTimeRangeExceededException(String requestId, String queryLength, String limit) {
        super(requestId, ErrorCode.LOKI_TIME_RANGE_EXCEEDED,
            String.format("조회 시간 범위가 Loki 서버의 제한을 초과했습니다. (요청 기간: %s, 최대 허용 기간: %s) 더 짧은 시간 범위로 다시 시도해주세요.", 
                queryLength, limit));
        this.queryLength = queryLength;
        this.limit = limit;
    }
}