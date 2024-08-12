package mcmp.mc.observability.agent.monitoring.enums;

public enum OS {
    LINUX,
    WINDOWS,
    MACOS,
    UNIX,
    SOLARIS,
    ;

    public static OS parse(String name) {
        for( OS t : OS.values() ) {
            if( t.name().equalsIgnoreCase(name) ) return t;
        }
        return null;
    }

    public static OS parseProperty() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return WINDOWS;

        } else if (os.contains("mac")) {
            return MACOS;

        } else if (os.contains("linux")) {
            return LINUX;

        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return UNIX;

        } else if (os.contains("sunos")) {
            return SOLARIS;
        }
        return null;
    }
}