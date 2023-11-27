package mcmp.mc.observability.agent.enums;

public enum StorageKind {
    INFLUXDB_V1
    ;

    public static StorageKind parse(String name) {
        if( name != null ) name = name.trim().replaceAll(" ", "_");
        for( StorageKind h : StorageKind.values() ) {
            if( h.name().equalsIgnoreCase(name) ) return h;
        }
        return null;
    }
}