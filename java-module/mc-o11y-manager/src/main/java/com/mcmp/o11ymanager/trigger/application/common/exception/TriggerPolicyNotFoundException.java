package com.mcmp.o11ymanager.trigger.application.common.exception;

/**
 * Exception thrown when a trigger policy cannot be found This exception is thrown when attempting
 * to access a trigger policy that does not exist.
 */
public class TriggerPolicyNotFoundException extends McO11yTriggerException {

    /**
     * Constructs a new exception for trigger policy not found by ID.
     *
     * @param id the ID of the trigger policy that was not found
     */
    public TriggerPolicyNotFoundException(Long id) {
        super("TriggerPolicy not found with id: " + id);
    }

    /**
     * Constructs a new exception for trigger policy not found by title.
     *
     * @param title the title of the trigger policy that was not found
     */
    public TriggerPolicyNotFoundException(String title) {
        super("TriggerPolicy not found with title: " + title);
    }

    @Override
    public String getErrorCode() {
        return "TRIGGER_POLICY_NOT_FOUND";
    }
}
