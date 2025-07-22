package com.mcmp.o11ymanager.oldService.domain.interfaces;

import com.mcmp.o11ymanager.model.log.Label;
import com.mcmp.o11ymanager.model.log.Log;
import com.mcmp.o11ymanager.model.log.LogCriteria;
import com.mcmp.o11ymanager.model.log.LogVolume;

public interface LokiService {

    Log getRangeLogs(LogCriteria criteria);

    Label getLabels(String start, String end, String query);

    Label getLabelValues(String label, String start, String end, String since, String query);

    LogVolume getLogVolumes(String query, String start, String end, Integer limit);
}
