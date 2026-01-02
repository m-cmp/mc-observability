package com.mcmp.o11ymanager.manager.infrastructure.port.ssh;

import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import com.mcmp.o11ymanager.manager.port.SshPort;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SshAdapter implements SshPort {

    private final ApacheSshClient apacheSshClient;

    @Override
    public SshConnection openSessionWithPrivateKey(
            String user, String ip, int port, Path privateKeyPath) throws IOException {
        return apacheSshClient.openSessionWithPrivateKey(user, ip, port, privateKeyPath);
    }

    @Override
    public SshConnection openSessionWithPrivateKeyString(
            String user, String ip, int port, String privateKeyContent) throws IOException {
        return apacheSshClient.openSessionWithPrivateKeyString(user, ip, port, privateKeyContent);
    }

    @Override
    public AgentCommandResult executeCommand(SshConnection connection, String command)
            throws Exception {
        return apacheSshClient.executeCommand(connection, command);
    }
}
