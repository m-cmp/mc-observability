package com.mcmp.o11ymanager.manager.model.log;

import lombok.Builder;
import lombok.Getter;

/** Domain model representing log search criteria */
@Getter
@Builder
public class LogCriteria {
  private final String query;
  private final int limit;
  private final String start;
  private final String end;
  private final String direction;
  private final String interval;
  private final String step;
  private final String since;

  /** Create basic log search criteria */
  public static LogCriteria of(String query, int limit) {
    return LogCriteria.builder().query(query).limit(limit).build();
  }

  /** Create log search criteria with a time range */
  public static LogCriteria ofRange(
      String query,
      String start,
      String end,
      int limit,
      String direction,
      String interval,
      String step,
      String since) {
    return LogCriteria.builder()
        .query(query)
        .start(start)
        .end(end)
        .limit(limit)
        .direction(direction)
        .interval(interval)
        .step(step)
        .since(since)
        .build();
  }
}
