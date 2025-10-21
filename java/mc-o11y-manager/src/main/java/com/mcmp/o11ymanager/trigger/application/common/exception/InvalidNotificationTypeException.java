package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when an invalid notification type is provided This exception is thrown when the
 * notification type is not supported or recognized.
 */
public class InvalidNotificationTypeException extends McO11yTriggerException {

    /**
     * Constructs a new exception for invalid notification type.
     *
     * @param notificationType the invalid notification type value
     */
    public InvalidNotificationTypeException(String notificationType) {
        super("Invalid notification type: " + notificationType);
    }

    @Override
    public String getErrorCode() {
        return "INVALID_NOTIFICATION_TYPE";
    }
}
