package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when a trigger history cannot be found This exception is thrown when attempting
 * to access a trigger history that does not exist.
 */
public class TriggerHistoryNotFoundException extends McO11yTriggerException {

    /**
     * Constructs a new exception for trigger history not found by ID.
     *
     * @param id the ID of the trigger history that was not found
     */
    public TriggerHistoryNotFoundException(Long id) {
        super("TriggerHistory not found with id: " + id);
    }

    @Override
    public String getErrorCode() {
        return "TRIGGER_HISTORY_NOT_FOUND";
    }
}
