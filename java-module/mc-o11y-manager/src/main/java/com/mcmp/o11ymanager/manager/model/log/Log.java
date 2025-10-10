package com.mcmp.o11ymanager.manager.model.log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class Log {

  private final String status;
  private final LogData logData;

  /**
   * Check if the response is successful
   */
  public boolean isSuccess() {
    return "success".equals(status);
  }

  /**
   * Inner class representing log data
   */
  @Getter
  @Builder
  public static class LogData {

    private final String resultType;
    private final List<LogResult> results;
    private final Object stats;

    /**
     * Check if the result type is vector
     */
    public boolean isVectorType() {
      return "vector".equals(resultType);
    }

    /**
     * Check if the result type is streams
     */
    public boolean isStreamsType() {
      return "streams".equals(resultType);
    }
  }

  /**
   * Interface representing a log result
   */
  public interface LogResult {

    /**
     * Return label information associated with the log
     */
    Map<String, String> getLabels();
  }

  /**
   * Vector-type log result
   */
  @Getter
  @Builder
  public static class VectorLogResult implements LogResult {

    private final Map<String, String> labels;
    private final double timestamp;
    private final String value;

    @Override
    public Map<String, String> getLabels() {
      return labels != null ? labels : Collections.emptyMap();
    }
  }

  /**
   * Stream-type log result
   */
  @Getter
  @Builder
  public static class StreamLogResult implements LogResult {

    private final Map<String, String> labels;
    private final List<LogEntry> entries;

    @Override
    public Map<String, String> getLabels() {
      return labels != null ? labels : Collections.emptyMap();
    }

    /**
     * Log entry
     */
    @Getter
    @Builder
    public static class LogEntry {

      private final String timestamp;
      private final String logLine;
    }
  }


}
