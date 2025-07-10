package com.innogrid.tabcloudit.o11ymanager.infrastructure.port.ssh;

import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.AgentCommandResult;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ApacheSshClient {

    @Value("${ssh.connection-timeout}")
    private int sshConnectionTimeout;

    public SshConnection openSession(String user, String ip, int port, String password) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        ClientSession session = client.connect(user, ip, port)
                .verify(sshConnectionTimeout, TimeUnit.MILLISECONDS)
                .getSession();

        try {
            session.addPasswordIdentity(password);
            session.auth().verify(sshConnectionTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IOException("Authentication failed: " + e.getMessage(), e);
        }

        String sessionId = UUID.randomUUID().toString();
        return new SshConnection(client, session, sessionId, port, ip);
    }

    public AgentCommandResult executeCommand(SshConnection connection, String command) throws Exception {
        ClientSession session = connection.getSession();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
             ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_EXEC, command)) {

            channel.setOut(outputStream);
            channel.setErr(errorStream);

            channel.open().verify(sshConnectionTimeout, TimeUnit.MILLISECONDS);
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EXIT_STATUS),
                    TimeUnit.MILLISECONDS.toMillis(sshConnectionTimeout));

            String output = outputStream.toString().trim();
            String error = errorStream.toString().trim();
            Integer exitStatus = channel.getExitStatus();

            return AgentCommandResult.builder().output(output).error(error).exitCode(exitStatus).build();
        }
    }
}
