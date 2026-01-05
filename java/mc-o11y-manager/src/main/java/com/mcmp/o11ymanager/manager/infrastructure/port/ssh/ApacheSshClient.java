package com.mcmp.o11ymanager.manager.infrastructure.port.ssh;

import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApacheSshClient {

    @Value("${ssh.connection-timeout}")
    private int sshConnectionTimeout;

    // Singleton SshClient - prevents thread leak
    private volatile SshClient sharedClient;
    private final Object clientLock = new Object();

    private SshClient getOrCreateClient() {
        if (sharedClient == null || !sharedClient.isStarted()) {
            synchronized (clientLock) {
                if (sharedClient == null || !sharedClient.isStarted()) {
                    sharedClient = SshClient.setUpDefaultClient();
                    sharedClient.start();
                }
            }
        }
        return sharedClient;
    }

    public SshConnection openSessionWithPrivateKey(
            String user, String ip, int port, Path privateKeyPath) throws IOException {
        SshClient client = getOrCreateClient();

        ClientSession session =
                client.connect(user, ip, port)
                        .verify(sshConnectionTimeout, TimeUnit.MILLISECONDS)
                        .getSession();

        try {
            FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(privateKeyPath);
            keyPairProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
            session.setKeyIdentityProvider(keyPairProvider);
            session.auth().verify(sshConnectionTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IOException("Authentication with private key failed: " + e.getMessage(), e);
        }

        String sessionId = UUID.randomUUID().toString();
        return new SshConnection(client, session, sessionId, port, ip);
    }

    public SshConnection openSessionWithPrivateKeyString(
            String user, String ip, int port, String privateKeyContent) throws IOException {
        SshClient client = getOrCreateClient();

        ClientSession session =
                client.connect(user, ip, port)
                        .verify(sshConnectionTimeout, TimeUnit.MILLISECONDS)
                        .getSession();

        try {
            // Parse private key from string content
            Iterable<KeyPair> keyPairs =
                    SecurityUtils.loadKeyPairIdentities(
                            null,
                            null,
                            new ByteArrayInputStream(
                                    privateKeyContent.getBytes(StandardCharsets.UTF_8)),
                            null);

            for (KeyPair keyPair : keyPairs) {
                session.addPublicKeyIdentity(keyPair);
            }

            session.auth().verify(sshConnectionTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IOException(
                    "Authentication with private key string failed: " + e.getMessage(), e);
        }

        String sessionId = UUID.randomUUID().toString();
        return new SshConnection(client, session, sessionId, port, ip);
    }

    public AgentCommandResult executeCommand(SshConnection connection, String command)
            throws Exception {
        ClientSession session = connection.getSession();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                ClientChannel channel =
                        session.createChannel(ClientChannel.CHANNEL_EXEC, command)) {

            channel.setOut(outputStream);
            channel.setErr(errorStream);

            channel.open().verify(sshConnectionTimeout, TimeUnit.MILLISECONDS);
            channel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EXIT_STATUS),
                    TimeUnit.MILLISECONDS.toMillis(sshConnectionTimeout));

            String output = outputStream.toString().trim();
            String error = errorStream.toString().trim();
            Integer exitStatus = channel.getExitStatus();
            if (exitStatus == null) {
                exitStatus = -1;
            }

            return AgentCommandResult.builder()
                    .output(output)
                    .error(error)
                    .exitCode(exitStatus)
                    .build();
        }
    }
}
