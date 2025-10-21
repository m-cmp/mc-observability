package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogCriteria;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import com.mcmp.o11ymanager.manager.port.LokiPort;
import com.mcmp.o11ymanager.manager.service.interfaces.LokiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LokiServiceImpl implements LokiService {

    private final LokiPort lokiPort;

    public Log getRangeLogs(LogCriteria criteria) {
        return lokiPort.fetchLogs(
                criteria.getQuery(),
                criteria.getStart(),
                criteria.getEnd(),
                criteria.getLimit(),
                criteria.getDirection(),
                criteria.getInterval(),
                criteria.getStep(),
                criteria.getSince());
    }

    public Label getLabels(String start, String end, String query) {
        return lokiPort.fetchLabels(start, end, query);
    }

    public Label getLabelValues(
            String label, String start, String end, String since, String query) {
        return lokiPort.fetchLabelValues(label, start, end, since, query);
    }

    public LogVolume getLogVolumes(String query, String start, String end, Integer limit) {
        return lokiPort.fetchLogVolumes(query, start, end, limit);
    }
}
