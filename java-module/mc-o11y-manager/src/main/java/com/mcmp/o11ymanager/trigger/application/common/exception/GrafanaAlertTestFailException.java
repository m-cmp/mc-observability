package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when Grafana alert rule creation fails This exception is used to indicate
 * failures during the alert rule creation process, including validation failures that trigger
 * rollback operations.
 */
public class GrafanaAlertTestFailException extends McO11yTriggerException {

    private static final String ERROR_CODE = "GRAFANA_ALERT_CREATION_FAILED";

    /**
     * Constructs a new Grafana alert creation exception with the specified detail message.
     *
     * @param message the detail message
     */
    public GrafanaAlertTestFailException(String message) {
        super(message);
    }

    /**
     * Constructs a new Grafana alert creation exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public GrafanaAlertTestFailException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the error code for this exception.
     *
     * @return the error code "GRAFANA_ALERT_CREATION_FAILED"
     */
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
