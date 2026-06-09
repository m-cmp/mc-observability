package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import java.util.List;

public interface TraceService {

    List<TraceResponseDto.TraceSummary> searchTraces(
            String traceQl, Integer limit, Long startSec, Long endSec);

    TraceResponseDto.TraceDetail getTraceDetail(String traceId);

    List<String> getServiceNames();
}
