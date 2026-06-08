package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import com.mcmp.o11ymanager.manager.port.TempoPort;
import com.mcmp.o11ymanager.manager.service.interfaces.TraceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TraceServiceImpl implements TraceService {

    private final TempoPort tempoPort;

    @Override
    public List<TraceResponseDto.TraceSummary> searchTraces(
            String traceQl, Integer limit, Long startSec, Long endSec) {
        return tempoPort.searchTraces(traceQl, limit, startSec, endSec);
    }

    @Override
    public TraceResponseDto.TraceDetail getTraceDetail(String traceId) {
        return tempoPort.getTraceDetail(traceId);
    }
}
