package com.mcmp.o11ymanager.manager.service;

import static com.mcmp.o11ymanager.manager.constants.WinRmConstants.isWinRmPort;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.agent.AgentStatusException;
import com.mcmp.o11ymanager.manager.exception.agent.SshConnectionException;
import com.mcmp.o11ymanager.manager.exception.config.FileReadingException;
import com.mcmp.o11ymanager.manager.facade.FileFacadeService;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.model.agentHealth.AgentCommandResult;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import com.mcmp.o11ymanager.manager.port.SshPort;
import com.mcmp.o11ymanager.manager.service.interfaces.SshService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SshServiceImpl implements SshService {

    private final FileFacadeService fileFacadeService;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    private final SshPort sshPort;
    private final RequestInfo requestInfo;

    @Value("${health.ssh-connection-cache-timeout:30000}")
    private long sshConnectionCacheTimeout;

    private final int MAX_RETRIES = 5;
    private final long RETRY_DELAY_MS = 500;
    public static final int SSH_COMMAND_EXECUTE_FAILED_CODE = -999;

    private final Map<String, SshConnection> connectionCache = new ConcurrentHashMap<>();
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();

    private Object getConnectionLock(String cacheKey) {
        return connectionLocks.computeIfAbsent(cacheKey, k -> new Object());
    }

    private String getTelegrafServiceName() {
        return "cmp-telegraf-" + deploySiteCode + ".service";
    }

    private String getFluentBitServiceName() {
        return "cmp-fluent-bit-" + deploySiteCode + ".service";
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupConnectionCache() {
        long currentTime = System.currentTimeMillis();
        connectionCache
                .entrySet()
                .removeIf(
                        entry -> {
                            if (currentTime - entry.getValue().getLastUsedTime()
                                    > sshConnectionCacheTimeout) {
                                try {
                                    entry.getValue().close();
                                    log.debug("Closed expired SSH connection: {}", entry.getKey());
                                } catch (Exception e) {
                                    log.warn(
                                            "Failed to close SSH connection: {}",
                                            entry.getKey(),
                                            e);
                                }
                                return true;
                            }
                            return false;
                        });
    }

    @Override
    public SshConnection getConnectionWithPrivateKey(
            String ip, int port, String user, Path privateKeyPath) {
        if (isWinRmPort(port)) {
            log.debug(
                    "Windows host detected (WinRM port {}), skipping SSH connection for {}",
                    port,
                    ip);
            return null;
        }

        String cacheKey = ip + ":" + port + ":" + user;

        // Check cache for active connection first (without lock)
        SshConnection connection = connectionCache.get(cacheKey);
        if (connection != null && connection.isActive()) {
            connection.updateLastUsedTime();
            return connection;
        }

        // Synchronize with host-based lock to prevent concurrent connection attempts
        synchronized (getConnectionLock(cacheKey)) {
            // Double-check (another thread may have already connected)
            connection = connectionCache.get(cacheKey);
            if (connection != null && connection.isActive()) {
                connection.updateLastUsedTime();
                return connection;
            }

            // Clean up inactive connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    log.debug("Error closing stale connection: {}", e.getMessage());
                }
                connectionCache.remove(cacheKey);
            }

            try {
                connection = sshPort.openSessionWithPrivateKey(user, ip, port, privateKeyPath);
                connectionCache.put(cacheKey, connection);
                connection.updateLastUsedTime();
                return connection;
            } catch (Exception e) {
                log.error(
                        "Error establishing SSH connection with private key to {}: {}",
                        ip,
                        e.getMessage());
                throw new SshConnectionException(requestInfo.getRequestId(), ip);
            }
        }
    }

    @Override
    public SshConnection getConnectionWithPrivateKeyString(
            String ip, int port, String user, String privateKeyContent) {
        if (isWinRmPort(port)) {
            log.debug(
                    "Windows host detected (WinRM port {}), skipping SSH connection for {}",
                    port,
                    ip);
            return null;
        }

        String cacheKey = ip + ":" + port + ":" + user;

        // Check cache for active connection first (without lock)
        SshConnection connection = connectionCache.get(cacheKey);
        if (connection != null && connection.isActive()) {
            connection.updateLastUsedTime();
            return connection;
        }

        // Synchronize with host-based lock to prevent concurrent connection attempts
        synchronized (getConnectionLock(cacheKey)) {
            // Double-check (another thread may have already connected)
            connection = connectionCache.get(cacheKey);
            if (connection != null && connection.isActive()) {
                connection.updateLastUsedTime();
                return connection;
            }

            // Clean up inactive connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    log.debug("Error closing stale connection: {}", e.getMessage());
                }
                connectionCache.remove(cacheKey);
            }

            try {
                connection =
                        sshPort.openSessionWithPrivateKeyString(user, ip, port, privateKeyContent);
                connectionCache.put(cacheKey, connection);
                connection.updateLastUsedTime();
                return connection;
            } catch (Exception e) {
                log.debug(
                        "Error establishing SSH connection with private key string to {}: {}",
                        ip,
                        e.getMessage());
                throw new SshConnectionException(requestInfo.getRequestId(), ip);
            }
        }
    }

    @Override
    public String runCommandWithPrivateKey(
            String ip, int port, String user, Path privateKeyPath, String command) {
        if (isWinRmPort(port)) {
            log.debug(
                    "Windows host (WinRM port {}), skipping SSH command execution for {}",
                    port,
                    ip);
            return "";
        }

        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                SshConnection sshConnection =
                        getConnectionWithPrivateKey(ip, port, user, privateKeyPath);

                if (sshConnection == null) {
                    retryCount++;
                    continue;
                }

                AgentCommandResult agentCommandResult =
                        sshPort.executeCommand(sshConnection, command);
                String output = agentCommandResult.getOutput();
                Integer exitCode = agentCommandResult.getExitCode();

                if (exitCode.equals(0)) {
                    return output;
                }

                retryCount++;
            } catch (Exception e) {
                removeConnection(ip, port, user);
                retryCount++;
            }

            if (retryCount < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "";
                }
            }
        }

        log.debug(
                "Failed to execute command with private key on host {} after {} retries",
                ip,
                MAX_RETRIES);

        return "";
    }

    @Override
    public String runCommandWithPrivateKeyString(
            String ip, int port, String user, String privateKeyContent, String command) {
        if (isWinRmPort(port)) {
            log.debug(
                    "Windows host (WinRM port {}), skipping SSH command execution for {}",
                    port,
                    ip);
            return "";
        }

        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                SshConnection sshConnection =
                        getConnectionWithPrivateKeyString(ip, port, user, privateKeyContent);

                if (sshConnection == null) {
                    retryCount++;
                    continue;
                }

                AgentCommandResult agentCommandResult =
                        sshPort.executeCommand(sshConnection, command);
                String output = agentCommandResult.getOutput();
                Integer exitCode = agentCommandResult.getExitCode();

                if (exitCode.equals(0)) {
                    return output;
                }

                retryCount++;
            } catch (Exception e) {
                removeConnection(ip, port, user);
                retryCount++;
            }

            if (retryCount < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "";
                }
            }
        }

        log.debug(
                "Failed to execute command with private key string on host {} after {} retries",
                ip,
                MAX_RETRIES);

        return "";
    }

    @Override
    public boolean isEnable(
            Agent agent, String ip, int port, String user, SshConnection sshConnection) {
        if (isWinRmPort(port)) {
            log.debug("Windows host (WinRM port {}), skipping SSH agent check for {}", port, ip);
            return false;
        }

        if (sshConnection == null) {
            return false;
        }

        String command = "";
        if (agent.equals(Agent.TELEGRAF)) {
            command = "sudo -n systemctl is-enabled " + getTelegrafServiceName();
        } else if (agent.equals(Agent.FLUENT_BIT)) {
            command = "sudo -n systemctl is-enabled " + getFluentBitServiceName();
        }
        try {
            AgentCommandResult result = sshPort.executeCommand(sshConnection, command);
            String response = result.getOutput().trim();
            return response.equalsIgnoreCase("enabled");
        } catch (Exception e) {
            log.error("Failed to check agent status: {}", agent, e);
            removeConnection(ip, port, user);
            return false;
        }
    }

    @Override
    public void removeConnection(String ip, int port, String user) {
        String cacheKey = ip + ":" + port + ":" + user;
        SshConnection conn = connectionCache.remove(cacheKey);
        if (conn != null) {
            try {
                conn.close();
            } catch (IOException ioException) {
                log.error("Error closing connection: {}", ioException.getMessage());
            }
        }
    }

    @Override
    public boolean existDirectory(SshConnection connection, String path) {
        if (connection == null) {
            log.debug("SSH connection is null (likely Windows host), skipping directory check");
            return false;
        }

        try {
            String command = "test -d " + path + " && echo EXISTS || echo NOT_EXISTS";
            AgentCommandResult result = sshPort.executeCommand(connection, command);

            return "EXISTS".equals(result.getOutput().trim());
        } catch (Exception e) {
            log.warn("Error checking if directory exists on remote: {}", e.getMessage());
            throw new FileReadingException(
                    "Error checking if directory exists on remote: " + e.getMessage());
        }
    }

    @Override
    public boolean checkDirectoryExistsFromRemote(SshConnection connection, String dirPath) {
        if (connection == null) {
            log.debug("SSH connection is null (likely Windows host), skipping directory check");
            return false;
        }

        try {
            String command = "test -d " + dirPath + " && echo 'EXISTS' || echo 'NOT_EXISTS'";
            AgentCommandResult agentCommandResult = sshPort.executeCommand(connection, command);

            return "EXISTS".equals(agentCommandResult.getOutput());
        } catch (Exception e) {
            log.warn("Error checking if directory exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkFluentBitDirectoryExistsFromRemote(SshConnection connection) {
        if (connection == null) {
            log.debug(
                    "SSH connection is null (likely Windows host), skipping fluent-bit directory check");
            return false;
        }

        try {
            String command =
                    "test -d "
                            + "/cmp-agent/fluent-bit/etc/fluent-bit"
                            + " && echo 'EXISTS' || echo 'NOT_EXISTS'";
            AgentCommandResult agentCommandResult = sshPort.executeCommand(connection, command);

            return "EXISTS".equals(agentCommandResult.getOutput());
        } catch (Exception e) {
            log.warn("Error checking if telegraf config exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isExistTelegrafConfigDirectory(SshConnection connection) {
        if (connection == null) {
            log.debug(
                    "SSH connection is null (likely Windows host), skipping telegraf config check");
            return false;
        }

        try {
            String command =
                    "test -d "
                            + fileFacadeService.getHostConfigTelegrafRemotePath()
                            + " && echo 'EXISTS' || echo 'NOT_EXISTS'";
            AgentCommandResult agentCommandResult = sshPort.executeCommand(connection, command);

            log.info("Checking if telegraf config exists: {}", agentCommandResult.getOutput());
            return "EXISTS".equals(agentCommandResult.getOutput());
        } catch (Exception e) {
            log.warn("Error checking if telegraf config exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isExistFluentbitConfigDirectory(SshConnection connection) {
        if (connection == null) {
            log.debug(
                    "SSH connection is null (likely Windows host), skipping fluent-bit config check");
            return false;
        }

        try {
            String command =
                    "test -d "
                            + fileFacadeService.getHostConfigFluentBitRemotePath()
                            + " && echo 'EXISTS' || echo 'NOT_EXISTS'";
            AgentCommandResult agentCommandResult = sshPort.executeCommand(connection, command);

            log.info("Checking if fluent-bit config exists: {}", agentCommandResult.getOutput());
            return "EXISTS".equals(agentCommandResult.getOutput());
        } catch (Exception e) {
            log.warn("Error checking if fluent-bit config exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listDirectory(SshConnection connection, String directoryPath)
            throws IOException {
        if (connection == null) {
            log.debug("SSH connection is null (likely Windows host), skipping directory list");
            return List.of();
        }

        String listCmd = "ls -1p " + directoryPath;
        AgentCommandResult listRes;
        try {
            listRes = sshPort.executeCommand(connection, listCmd);

        } catch (Exception e) {
            log.error("Failed to list directory {}: {}", directoryPath, e.getMessage());
            throw new IOException("Failed to retrieve directory list: " + directoryPath, e);
        }
        return Arrays.stream(listRes.getOutput().split("\\r?\\n"))
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }

    @Override
    public String readFileContent(SshConnection connection, String filePath) throws IOException {
        if (connection == null) {
            log.debug("SSH connection is null (likely Windows host), skipping file read");
            return "";
        }

        String catCmd = "cat " + filePath;
        AgentCommandResult catRes;
        try {
            catRes = sshPort.executeCommand(connection, catCmd);
        } catch (Exception e) {
            log.error("Failed to read file {}: {}", filePath, e.getMessage());
            throw new IOException("Failed to read file: " + filePath, e);
        }
        return catRes.getOutput();
    }

    @Override
    public void download(
            SshConnection connection,
            String remoteFilePath,
            Path localPath,
            String username,
            String ip,
            int port,
            String privateKeyContent)
            throws IOException {

        if (connection == null) {
            log.debug("SSH connection is null (likely Windows host), skipping file download");
            return;
        }

        if (!Files.exists(localPath)) {
            Files.createDirectories(localPath);
            log.info("Local directory created: {}", localPath);
        } else if (!Files.isDirectory(localPath)) {
            log.error("The specified local path is not a directory: {}", localPath);
            return;
        }

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session =
                    client.connect(username, ip, port).verify(15, TimeUnit.SECONDS).getSession()) {

                Iterable<KeyPair> keyPairs;
                try {
                    keyPairs =
                            SecurityUtils.loadKeyPairIdentities(
                                    null,
                                    null,
                                    new ByteArrayInputStream(
                                            privateKeyContent.getBytes(StandardCharsets.UTF_8)),
                                    null);
                } catch (GeneralSecurityException e) {
                    throw new IOException("Failed to load private key: " + e.getMessage(), e);
                }

                for (KeyPair keyPair : keyPairs) {
                    session.addPublicKeyIdentity(keyPair);
                }

                if (session.auth().verify(15, TimeUnit.SECONDS).isFailure()) {
                    throw new IOException("SSH server authentication failed: " + ip);
                }
                log.info("SSH session authentication successful. {}", ip);

                SftpClientFactory factory = SftpClientFactory.instance();

                try (SftpClient sftpClient = factory.createSftpClient(session)) {
                    log.info(
                            "SFTP client created. Starting download: {} -> {}",
                            remoteFilePath,
                            localPath);
                    recursiveDownload(sftpClient, remoteFilePath, localPath);
                    log.info("Download completed: {} -> {}", remoteFilePath, localPath);
                }
            }
        }
    }

    private void recursiveDownload(
            SftpClient sftpClient, String currentRemotePath, Path currentLocalPath)
            throws IOException {

        Iterable<SftpClient.DirEntry> entries;
        try {
            entries = sftpClient.readDir(currentRemotePath);
        } catch (IOException e) {
            log.error(
                    "Error: Failed to read remote directory {} : {}",
                    currentRemotePath,
                    e.getMessage());
            return;
        }

        for (SftpClient.DirEntry entry : entries) {
            String entryName = entry.getFilename();
            if (entryName.equals(".") || entryName.equals("..")) {
                continue;
            }

            String nextRemotePath = sftpClient.canonicalPath(currentRemotePath + "/" + entryName);
            Path nextLocalPath = currentLocalPath.resolve(entryName);
            SftpClient.Attributes attrs = entry.getAttributes();

            if (attrs.isDirectory()) {
                if (!Files.exists(nextLocalPath)) {
                    Files.createDirectories(nextLocalPath);
                    log.info("Directory created: {}", nextLocalPath);
                }
                log.info("Entering directory: {}", nextRemotePath);
                recursiveDownload(sftpClient, nextRemotePath, nextLocalPath);
            } else if (attrs.isRegularFile()) {
                log.info("Downloading file: {} -> {}", nextRemotePath, nextLocalPath);
                try (InputStream inputStream = sftpClient.read(nextRemotePath)) {
                    Files.copy(inputStream, nextLocalPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(
                            "Error: Failed to download file {} : {}",
                            nextRemotePath,
                            e.getMessage());
                }
            } else if (attrs.isSymbolicLink()) {
                log.info("Skipping symbolic link: {}", nextRemotePath);
            } else {
                log.info("Skipping unsupported file type: {}", nextRemotePath);
            }
        }
    }

    @Override
    public void enableFluentBit(SshConnection connection, String ip, int port, String user) {

        String enableCommand = "sudo -n systemctl enable --now " + getFluentBitServiceName();

        AgentCommandResult enableRes;

        boolean isAlreadyEnabled;

        try {
            isAlreadyEnabled = isEnable(Agent.FLUENT_BIT, ip, port, user, connection);

            if (isAlreadyEnabled) {
                log.info("Fluent Bit service is already enabled. No need to enable.");
                return;
            }

            enableRes = sshPort.executeCommand(connection, enableCommand);

            if (enableRes.getExitCode() == 0) {
                log.info("Successfully enabled Fluent Bit service.");
            } else {
                log.error(
                        "Failed to enable Fluent Bit service. ExitCode: {}, Error: {}",
                        enableRes.getExitCode(),
                        enableRes.getError());
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Failed to enable Fluent Bit service",
                        Agent.FLUENT_BIT);
            }
        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during Fluent Bit enablement", e);
        }
    }

    @Override
    public void disableFluentBit(SshConnection connection, String ip, int port, String user) {

        String disableCommand = "sudo -n systemctl disable --now " + getFluentBitServiceName();

        AgentCommandResult disableRes;

        try {
            disableRes = sshPort.executeCommand(connection, disableCommand);

            if (disableRes.getExitCode() == 0) {
                log.info("Successfully disabled Fluent Bit service.");
            } else {
                log.error(
                        "Failed to disable Fluent Bit service. ExitCode: {}, Error: {}",
                        disableRes.getExitCode(),
                        disableRes.getError());
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Failed to disable Fluent Bit service",
                        Agent.FLUENT_BIT);
            }
        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    @Override
    public void restartFluentBit(SshConnection connection, String ip, int port, String user) {

        String restartCommand = "sudo -n systemctl restart " + getFluentBitServiceName();

        try {
            if (!isEnable(Agent.FLUENT_BIT, ip, port, user, connection)) {
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Fluent Bit is not enabled. Cannot restart.",
                        Agent.FLUENT_BIT);
            }

            AgentCommandResult restartRes = sshPort.executeCommand(connection, restartCommand);

            if (restartRes.getExitCode() != 0) {
                throw new RuntimeException(
                        "Restart failed. ExitCode: "
                                + restartRes.getExitCode()
                                + ", Error: "
                                + restartRes.getError());
            }

            log.info("Successfully restarted Fluent Bit service.");

        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to restart Fluent Bit", e);
        }
    }

    @Override
    public void enableTelegraf(SshConnection connection, String ip, int port, String user) {

        String enableCommand = "sudo -n systemctl enable --now " + getTelegrafServiceName();

        AgentCommandResult enableRes;

        boolean isAlreadyEnabled;

        try {
            isAlreadyEnabled = isEnable(Agent.TELEGRAF, ip, port, user, connection);

            if (isAlreadyEnabled) {
                log.info("Telegraf service is already enabled. No need to enable.");
                return;
            }

            enableRes = sshPort.executeCommand(connection, enableCommand);

            if (enableRes.getExitCode() == 0) {
                log.info("Successfully enabled Telegraf service.");
            } else {
                log.error(
                        "Failed to enable Telegraf service. ExitCode: {}, Error: {}",
                        enableRes.getExitCode(),
                        enableRes.getError());
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Failed to enable Telegraf service",
                        Agent.TELEGRAF);
            }
        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during Telegraf enablement", e);
        }
    }

    @Override
    public void disableTelegraf(SshConnection connection, String ip, int port, String user) {

        String disableCommand = "sudo -n systemctl disable --now " + getTelegrafServiceName();

        AgentCommandResult disableRes;

        try {
            disableRes = sshPort.executeCommand(connection, disableCommand);

            if (disableRes.getExitCode() == 0) {
                log.info("Successfully disabled Telegraf service.");
            } else {
                log.error(
                        "Failed to disable Telegraf service. ExitCode: {}, Error: {}",
                        disableRes.getExitCode(),
                        disableRes.getError());
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Failed to disable Telegraf service.",
                        Agent.TELEGRAF);
            }
        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    @Override
    public void restartTelegraf(SshConnection connection, String ip, int port, String user) {

        String restartCommand = "sudo -n systemctl restart " + getTelegrafServiceName();

        try {
            if (!isEnable(Agent.TELEGRAF, ip, port, user, connection)) {
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Telegraf is not enabled. Cannot restart.",
                        Agent.TELEGRAF);
            }

            AgentCommandResult restartRes = sshPort.executeCommand(connection, restartCommand);

            if (restartRes.getExitCode() != 0) {
                throw new RuntimeException(
                        "Restart failed. ExitCode: "
                                + restartRes.getExitCode()
                                + ", Error: "
                                + restartRes.getError());
            }

            log.info("Successfully restarted Telegraf service.");

        } catch (AgentStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to restart Telegraf", e);
        }
    }
}
