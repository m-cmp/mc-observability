package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.model.log.Label;
import com.innogrid.tabcloudit.o11ymanager.model.log.Log;
import com.innogrid.tabcloudit.o11ymanager.model.log.LogCriteria;
import com.innogrid.tabcloudit.o11ymanager.model.log.LogVolume;

public interface LokiService {

    Log getRangeLogs(LogCriteria criteria);

    Label getLabels(String start, String end, String query);

    Label getLabelValues(String label, String start, String end, String since, String query);

    LogVolume getLogVolumes(String query, String start, String end, Integer limit);
}
