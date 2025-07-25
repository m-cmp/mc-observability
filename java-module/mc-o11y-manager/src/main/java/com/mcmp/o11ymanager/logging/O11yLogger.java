package com.mcmp.o11ymanager.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class O11yLogger {
  private final Logger logger;

  public static O11yLogger getLogger(Class<?> clazz) {
    return new O11yLogger(LoggerFactory.getLogger(clazz));
  }

  private O11yLogger(Logger logger) {
    this.logger = logger;
  }

  public void debug(String format, Object... args) {
    logger.debug(format, args);
    MDC.clear();
  }

  public void info(String format, Object... args) {
    logger.info(format, args);
    MDC.clear();
  }

  public void warn(String format, Object... args) {
    logger.warn(format, args);
    MDC.clear();
  }

  public void error(String format, Object... args) {
    logger.error(format, args);
    MDC.clear();
  }

  public void fatal(String format, Object... args) {
    MDC.put("severity", "FATAL");
    logger.error(format, args);
    MDC.clear();
  }

}

