package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;

/**
 * Interface (port) for interacting with the Loki log storage. Enables dependency inversion from the
 * domain layer to the infrastructure layer.
 */
public interface LokiPort {

    /**
     * Fetch log data.
     *
     * @param query log query
     * @param limit maximum number of logs to fetch
     * @return log response object
     */
    Log fetchLogs(String query, int limit);

    /**
     * Fetch log data within a specific time range.
     *
     * @param query log query
     * @param start start time
     * @param end end time
     * @param limit maximum number of logs to fetch
     * @param direction query direction (forward/backward)
     * @param interval minimum interval between individual log line (stream) results
     * @param step calculation/evaluation interval for metric queries (matrix)
     * @param since query start time relative to the end time (or current time)
     * @return log response object
     */
    Log fetchLogs(
            String query,
            String start,
            String end,
            int limit,
            String direction,
            String interval,
            String step,
            String since);

    /**
     * Fetch label list.
     *
     * @param start start time (optional)
     * @param end end time (optional)
     * @param query query (optional)
     * @return label list
     */
    Label fetchLabels(String start, String end, String query);

    /**
     * Fetch values for a specific label.
     *
     * @param label label name
     * @param start start time (optional)
     * @param end end time (optional)
     * @param since time since a specific point (optional)
     * @param query query (optional)
     * @return label value list
     */
    Label fetchLabelValues(String label, String start, String end, String since, String query);

    /**
     * Fetch log volume data.
     *
     * @param query log query
     * @param start start time
     * @param end end time
     * @param limit maximum number of results to return (optional)
     * @return log volume response object
     */
    LogVolume fetchLogVolumes(String query, String start, String end, Integer limit);
}
