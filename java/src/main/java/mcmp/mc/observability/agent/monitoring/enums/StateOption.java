package mcmp.mc.observability.agent.monitoring.enums;

public enum StateOption {
    NONE,
    ADD,
    UPDATE,
    DELETE,
    ;

    public static StateOption parse(String status) {
        status = status.toUpperCase();

        for( StateOption h : StateOption.values() ) {
            if( h.name().equals(status) ) return h;
        }
        return null;
    }
}