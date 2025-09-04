package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when an invalid alert level is provided This exception is thrown when the alert
 * level value is not within the allowed range or format.
 */
public class InvalidAlertLevelException extends McO11yTriggerException {

    /**
     * Constructs a new exception for invalid alert level.
     *
     * @param alertLevel the invalid alert level value
     */
    public InvalidAlertLevelException(String alertLevel) {
        super("Invalid alert level: " + alertLevel);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_ALERT_LEVEL";
    }
}
