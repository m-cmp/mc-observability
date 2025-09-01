package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when there's an error in notification configuration This exception is thrown
 * when notification settings are misconfigured or invalid.
 */
public class NotificationConfigurationException extends McO11yTriggerException {

    /**
     * Constructs a new exception for notification configuration error.
     *
     * @param message the detail message about the configuration error
     */
    public NotificationConfigurationException(String message) {
        super("Notification configuration error: " + message);
    }

    /**
     * Constructs a new exception for notification configuration error with cause.
     *
     * @param message the detail message about the configuration error
     * @param cause the underlying cause of the error
     */
    public NotificationConfigurationException(String message, Throwable cause) {
        super("Notification configuration error: " + message, cause);
    }

    @Override
    public String getErrorCode() {
        return "NOTIFICATION_CONFIGURATION_ERROR";
    }
}
