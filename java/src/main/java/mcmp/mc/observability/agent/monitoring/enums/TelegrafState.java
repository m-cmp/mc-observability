package mcmp.mc.observability.agent.monitoring.enums;

public enum TelegrafState {
    RUNNING,
    STOPPED,
    FAILED,
    ;

    public static TelegrafState parse(String s) {
        for( TelegrafState h :  TelegrafState.values() ) {
            if( h.name().equalsIgnoreCase(s) ) return h;
        }
        return null;
    }
}
