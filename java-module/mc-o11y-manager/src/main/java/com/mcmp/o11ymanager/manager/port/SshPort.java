package com.mcmp.o11ymanager.manager.port;
import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;

import java.io.IOException;

public interface SshPort {
    SshConnection openSession(String user, String ip, int port, String password) throws IOException;
    AgentCommandResult executeCommand(SshConnection connection, String command) throws Exception;
}
