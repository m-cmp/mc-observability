package mcmp.mc.observability.mco11yagent.monitoring.enums;

import lombok.Getter;

public enum Measurement {
    CPU_USAGE("cpu_usage"),
    MEMORY_USAGE("memory_usage"),
    DISK_READ("disk_read"),
    DISK_WRITE("disk_write"),
    DISK_READ_OPS("disk_read_ops"),
    DISK_WRITE_OPS("disk_write_ops"),
    NETWORK_IN("network_in"),
    NETWORK_OUT("network_out"),
    UNKNOWN("unknown");

    private final String value;

    Measurement(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
