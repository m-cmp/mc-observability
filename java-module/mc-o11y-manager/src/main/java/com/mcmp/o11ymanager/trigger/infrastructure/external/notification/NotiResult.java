package com.mcmp.o11ymanager.trigger.infrastructure.external.notification;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

/**
 * Result class for notification delivery operations Contains information about the success/failure
 * of notification delivery.
 */
@Getter
public class NotiResult {

    @Setter String channel;
    String recipients;
    String exception;
    boolean isSucceeded;

    /**
     * Creates a successful notification result.
     *
     * @param recipients the recipients who received the notification
     * @return successful notification result
     */
    public static NotiResult success(String recipients) {
        NotiResult notiResult = new NotiResult();
        notiResult.recipients = recipients;
        notiResult.isSucceeded = true;
        return notiResult;
    }

    /**
     * Creates a failed notification result.
     *
     * @param recipients the intended recipients
     * @param exception the exception that caused the failure
     * @return failed notification result
     */
    public static NotiResult fail(String recipients, Exception exception) {
        NotiResult notiResult = new NotiResult();
        notiResult.recipients = recipients;
        notiResult.exception = exToString(exception);
        notiResult.isSucceeded = false;
        return notiResult;
    }

    /**
     * Creates a partially failed notification result.
     *
     * @param recipients the intended recipients
     * @param exceptions the list of exceptions that occurred
     * @return partially failed notification result
     */
    public static NotiResult partialFail(String recipients, List<Exception> exceptions) {
        NotiResult notiResult = new NotiResult();
        notiResult.recipients = recipients;
        notiResult.exception =
                exceptions.stream().map(NotiResult::exToString).collect(Collectors.joining("\n"));
        notiResult.isSucceeded = false;
        return notiResult;
    }

    /**
     * Converts exception to string format including stack trace.
     *
     * @param exception the exception to convert
     * @return string representation of the exception
     */
    private static String exToString(Exception exception) {
        return exception.getMessage()
                + "\n\n"
                + Arrays.stream(exception.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
    }
}
