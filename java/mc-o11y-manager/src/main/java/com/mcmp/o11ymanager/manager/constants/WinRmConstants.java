package com.mcmp.o11ymanager.manager.constants;

public final class WinRmConstants {
    public static final int WINRM_HTTP_PORT = 5985;
    public static final int WINRM_HTTPS_PORT = 5986;

    private WinRmConstants() {}

    public static boolean isWinRmPort(int port) {
        return port == WINRM_HTTP_PORT || port == WINRM_HTTPS_PORT;
    }
}
