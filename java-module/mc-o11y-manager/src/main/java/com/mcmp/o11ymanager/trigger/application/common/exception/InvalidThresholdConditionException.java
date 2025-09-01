package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when an invalid threshold condition is provided This exception is thrown when
 * the threshold condition configuration is malformed or invalid.
 */
public class InvalidThresholdConditionException extends McO11yTriggerException {

    /**
     * Constructs a new exception for invalid threshold condition.
     *
     * @param message the detail message about the invalid condition
     * @param cause the underlying cause of the error
     */
    public InvalidThresholdConditionException(String message, Throwable cause) {
        super("Invalid threshold condition: " + message, cause);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_THRESHOLD_CONDITION";
    }
}
