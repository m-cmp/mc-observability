package com.mcmp.o11ymanager.trigger.application.common.type;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidAlertLevelException;
import java.util.Arrays;
import lombok.Getter;

/**
 * Enumeration for alert severity levels. Defines the available alert levels with their
 * corresponding numeric values.
 */
@Getter
public enum AlertLevel {
    INFO(1),
    WARNING(2),
    CRITICAL(3);

    private final int value;

    AlertLevel(int value) {
        this.value = value;
    }

    /**
     * Returns AlertLevel by numeric value.
     *
     * @param value numeric value to find
     * @return AlertLevel corresponding to the value
     * @throws InvalidAlertLevelException if no matching level found
     */
    public static AlertLevel valueOf(int value) {
        return Arrays.stream(values())
                .filter(level -> level.value == value)
                .findFirst()
                .orElseThrow(() -> new InvalidAlertLevelException(String.valueOf(value)));
    }

    /**
     * Returns AlertLevel by name string.
     *
     * @param name alert level name to find
     * @return AlertLevel corresponding to the name
     * @throws InvalidAlertLevelException if no matching level found
     */
    public static AlertLevel findBy(String name) {
        String upper = name.toUpperCase();
        return Arrays.stream(values())
                .filter(level -> level.name().equals(upper))
                .findFirst()
                .orElseThrow(() -> new InvalidAlertLevelException(upper));
    }
}
