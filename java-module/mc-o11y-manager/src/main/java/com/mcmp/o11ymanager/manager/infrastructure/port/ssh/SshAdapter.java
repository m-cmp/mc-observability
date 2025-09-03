package com.mcmp.o11ymanager.manager.infrastructure.port.ssh;

import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import com.mcmp.o11ymanager.manager.port.SshPort;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SshAdapter implements SshPort {

    private final ApacheSshClient apacheSshClient;

    @Override
    public SshConnection openSession(String user, String ip, int port, String password)
            throws IOException {
        return apacheSshClient.openSession(user, ip, port, password);
    }

    @Override
    public AgentCommandResult executeCommand(SshConnection connection, String command)
            throws Exception {
        return apacheSshClient.executeCommand(connection, command);
    }
}
