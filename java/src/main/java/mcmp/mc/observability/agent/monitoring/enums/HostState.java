package mcmp.mc.observability.agent.monitoring.enums;

public enum HostState {
    ACTIVE,
    INACTIVE,
    ;

    public static HostState parse(String s) {
        for( HostState h :  HostState.values() ) {
            if( h.name().equalsIgnoreCase(s) ) return h;
        }
        return null;
    }
}