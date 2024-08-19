package mcmp.mc.observability.agent.trigger.enums;

public enum TaskStatus {
    ENABLED,
    DISABLED
    ;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static TaskStatus parse(String status) {
        status = status.toUpperCase();

        for( TaskStatus h : TaskStatus.values() ) {
            if( h.name().equals(status) ) return h;
        }
        return null;
    }
}
