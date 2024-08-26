package mcmp.mc.observability.agent.monitoring.enums;

public enum StateYN {
    Y,N;

    public static StateYN parse(String name) {
        for( StateYN t :  StateYN.values() ) {
            if( t.name().equalsIgnoreCase(name) ) return t;
        }
        return null;
    }

    public static StateYN parse(boolean state) {
        if( state ) return Y;
        return N;
    }
}
