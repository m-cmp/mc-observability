package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when notification delivery fails This exception is thrown when there's an error
 * during the process of delivering notifications.
 */
public class NotificationDeliveryException extends McO11yTriggerException {

    /**
     * Constructs a new exception for notification delivery failure.
     *
     * @param message the detail message about the delivery failure
     */
    public NotificationDeliveryException(String message) {
        super("Notification delivery failed: " + message);
    }

    /**
     * Constructs a new exception for notification delivery failure with detailed information.
     *
     * @param channel the notification channel that failed
     * @param statusCode the HTTP status code returned
     * @param response the response message
     */
    public NotificationDeliveryException(String channel, int statusCode, String response) {
        super(
                String.format(
                        "Notification delivery failed for %s channel - Status: %d, Response: %s",
                        channel, statusCode, response));
    }

    @Override
    public String getErrorCode() {
        return "NOTIFICATION_DELIVERY_FAILED";
    }
}
