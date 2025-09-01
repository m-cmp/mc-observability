package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Base exception class for MC-O11Y Trigger application All custom exceptions in the application
 * should extend this class.
 */
public abstract class McO11yTriggerException extends RuntimeException {

    /**
     * Constructs a new MC-O11Y Trigger exception with the specified detail message.
     *
     * @param message the detail message
     */
    protected McO11yTriggerException(String message) {
        super(message);
    }

    /**
     * Constructs a new MC-O11Y Trigger exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    protected McO11yTriggerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the error code for this exception.
     *
     * @return the error code
     */
    public abstract String getErrorCode();
}
