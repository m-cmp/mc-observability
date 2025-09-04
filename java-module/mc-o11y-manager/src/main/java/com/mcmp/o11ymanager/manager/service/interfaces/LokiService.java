package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogCriteria;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;

public interface LokiService {

    Log getRangeLogs(LogCriteria criteria);

    Label getLabels(String start, String end, String query);

    Label getLabelValues(String label, String start, String end, String since, String query);

    LogVolume getLogVolumes(String query, String start, String end, Integer limit);
}
