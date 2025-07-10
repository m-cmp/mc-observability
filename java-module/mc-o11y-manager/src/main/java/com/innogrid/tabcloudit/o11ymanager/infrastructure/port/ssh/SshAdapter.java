package com.innogrid.tabcloudit.o11ymanager.infrastructure.port.ssh;

import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.AgentCommandResult;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.port.SshPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SshAdapter implements SshPort {

    private final ApacheSshClient apacheSshClient;

    @Override
    public SshConnection openSession(String user, String ip, int port, String password) throws IOException {
        return apacheSshClient.openSession(user, ip, port, password);
    }

    @Override
    public AgentCommandResult executeCommand(SshConnection connection, String command) throws Exception {
        return apacheSshClient.executeCommand(connection, command);
    }
}
