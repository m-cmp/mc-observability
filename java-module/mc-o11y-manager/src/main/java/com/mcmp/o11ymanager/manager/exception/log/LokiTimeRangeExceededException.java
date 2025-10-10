package com.mcmp.o11ymanager.manager.exception.log;

import com.mcmp.o11ymanager.manager.global.error.BaseException;
import lombok.Getter;

@Getter
public class LokiTimeRangeExceededException extends BaseException {

    private final String queryLength;
    private final String limit;

    public LokiTimeRangeExceededException(String requestId, String queryLength, String limit) {
        super(
                requestId,
                "The query time range exceeds the limit of the Loki server. Please try again with a shorter time range.");
        this.queryLength = queryLength;
        this.limit = limit;
    }
}
