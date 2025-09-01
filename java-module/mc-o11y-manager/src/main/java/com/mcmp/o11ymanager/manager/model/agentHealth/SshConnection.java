package com.mcmp.o11ymanager.manager.model.agentHealth;

import lombok.Getter;
import lombok.Setter;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;

@Getter
@Setter
public class SshConnection implements AutoCloseable {
    private final SshClient client;
    private final ClientSession session;
    private final String sessionId;
    private final int port;
    private final String ip;
    private long lastUsedTime;

    public SshConnection(SshClient client, ClientSession session, String sessionId, int port, String ip) {
        this.client = client;
        this.session = session;
        this.sessionId = sessionId;
        this.port = port;
        this.ip = ip;
        this.lastUsedTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        return session != null && session.isOpen() && !session.isClosed();
    }

    public void updateLastUsedTime() {
        this.lastUsedTime = System.currentTimeMillis();
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            session.close();
        }
        if (client != null) {
            client.stop();
        }
    }
}
