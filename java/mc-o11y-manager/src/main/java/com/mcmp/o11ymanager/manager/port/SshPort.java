package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import java.io.IOException;
import java.nio.file.Path;

public interface SshPort {
    SshConnection openSessionWithPrivateKey(String user, String ip, int port, Path privateKeyPath)
            throws IOException;

    SshConnection openSessionWithPrivateKeyString(
            String user, String ip, int port, String privateKeyContent) throws IOException;

    AgentCommandResult executeCommand(SshConnection connection, String command) throws Exception;
}
