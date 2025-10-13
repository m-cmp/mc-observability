package com.mcmp.o11ymanager.manager.service;

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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
                        entry ->
                                currentTime - entry.getValue().getLastUsedTime()
                                        > sshConnectionCacheTimeout);
    }

    @Override
    public SshConnection getConnection(String ip, int port, String user, String password) {
        String cacheKey = ip + ":" + port + ":" + user;
        SshConnection connection = connectionCache.get(cacheKey);

        if (connection != null) {
            if (connection.isActive()) {
                return connection;
            } else {
                try {
                    connection.close();
                } catch (IOException e) {
                    log.error("Error closing stale connection: {}", e.getMessage());
                    throw new SshConnectionException(
                            "Failed to disconnect SSH connection" + ip, user);
                }
            }
        }

        try {
            connection = sshPort.openSession(user, ip, port, password);
            connectionCache.put(cacheKey, connection);
            connection.updateLastUsedTime();

            return connection;
        } catch (Exception e) {
            log.error("Error establishing SSH connection to {}: {}", ip, e.getMessage());
            throw new SshConnectionException(
                    requestInfo.getRequestId(), "Failed to connect via SSH" + ip);
        }
    }

    @Override
    public void updateConnection(String ip, int port, String user, String password) {
        removeConnection(ip, port, user);
        getConnection(ip, port, user, password);
    }

    public String runCommand(String ip, int port, String user, String password, String command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            SshConnection sshConnection = getConnection(ip, port, user, password);

            if (sshConnection == null) {
                retryCount++;
                continue;
            }

            try {
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

        log.error("Error executing command on host {}: {}", ip, command);

        return "";
    }

    @Override
    public AgentCommandResult runCommandWithResult(
            String ip, int port, String user, String password, String command) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            SshConnection sshConnection = getConnection(ip, port, user, password);

            if (sshConnection == null) {
                retryCount++;
                continue;
            }

            try {
                return sshPort.executeCommand(sshConnection, command);
            } catch (Exception e) {
                removeConnection(ip, port, user);
                retryCount++;
            }

            if (retryCount < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.error("Error executing command on host {}: {}", ip, command);

        return AgentCommandResult.builder()
                .output("")
                .exitCode(SSH_COMMAND_EXECUTE_FAILED_CODE)
                .build();
    }

    @Override
    public boolean isEnable(
            Agent agent,
            String ip,
            int port,
            String user,
            String password,
            SshConnection sshConnection) {
        if (sshConnection == null) {
            return false;
        }

        String command = "";
        if (agent.equals(Agent.TELEGRAF)) {
            command =
                    "echo '"
                            + password
                            + "' | sudo -S -p '' systemctl is-enabled "
                            + getTelegrafServiceName();
        } else if (agent.equals(Agent.FLUENT_BIT)) {
            command =
                    "echo '"
                            + password
                            + "' | sudo -S -p '' systemctl is-enabled "
                            + getFluentBitServiceName();
        }
        try {
            AgentCommandResult result = runCommandWithResult(ip, port, user, password, command);
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
            String password)
            throws IOException {

        // Check if the local path is a directory, and create it if it doesn't exist
        if (!Files.exists(localPath)) {
            Files.createDirectories(localPath);
            log.info("The specified local path is not a directory: {}", localPath);
        } else if (!Files.isDirectory(localPath)) {
            log.error("지정된 로컬 경로가 디렉토리가 아닙니다: {}", localPath);
            return;
        }

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session =
                    client.connect(username, ip, port).verify(15, TimeUnit.SECONDS).getSession()) {

                session.addPasswordIdentity(password); // 비밀번호 인증

                // To use public key authentication:
                // session.addPublicKeyIdentity(UserInteraction.loadKeyIdentity("path/to/your/private_key_file", null, null));

                // Attempt authentication and set timeout

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

        // Read entries from the remote directory
        Iterable<SftpClient.DirEntry> entries;
        try {
            entries = sftpClient.readDir(currentRemotePath);
        } catch (IOException e) {
            log.error(
                    "Error: Failed to read remote directory {} : {}",
                    currentRemotePath,
                    e.getMessage());
            // The directory may be skipped due to incorrect path or insufficient permissions.
            return;
        }

        for (SftpClient.DirEntry entry : entries) {
            String entryName = entry.getFilename();
            // Skip current (.) and parent (..) directories
            if (entryName.equals(".") || entryName.equals("..")) {
                continue;
            }

            String nextRemotePath = sftpClient.canonicalPath(currentRemotePath + "/" + entryName);
            Path nextLocalPath = currentLocalPath.resolve(entryName);
            SftpClient.Attributes attrs = entry.getAttributes();

            if (attrs.isDirectory()) {
                // Create the corresponding local directory if it doesn't exist
                if (!Files.exists(nextLocalPath)) {
                    Files.createDirectories(nextLocalPath);
                    log.info("Directory created: {}", nextLocalPath);
                }
                log.info("Entering directory: {}", nextRemotePath);
                recursiveDownload(sftpClient, nextRemotePath, nextLocalPath); // Recursive call
            } else if (attrs.isRegularFile()) {
                log.info("Downloading file: {} -> {}", nextRemotePath, nextLocalPath);
                try (InputStream inputStream = sftpClient.read(nextRemotePath)) {
                    Files.copy(inputStream, nextLocalPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(
                            "Error: Failed to download file {} : {}",
                            nextRemotePath,
                            e.getMessage());
                    // Continue execution even if a file download fails
                }
            } else if (attrs.isSymbolicLink()) {
                log.info("Skipping symbolic link: {}", nextRemotePath);
                // Add symbolic link handling logic if necessary
            } else {
                log.info("Skipping unsupported file type: {}", nextRemotePath);
            }
        }
    }

    @Override
    // fluent-bit enable function
    public void enableFluentBit(
            SshConnection connection, String ip, int port, String user, String password) {

        String enableCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl enable --now "
                        + getFluentBitServiceName();

        AgentCommandResult enableRes;

        boolean isAlreadyEnabled;

        try {
            isAlreadyEnabled = isEnable(Agent.FLUENT_BIT, ip, port, user, password, connection);

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
    // fluent-bit disable function
    public void disableFluentBit(
            SshConnection connection, String ip, int port, String user, String password) {

        String disableCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl disable --now "
                        + getFluentBitServiceName();

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

    // fluent-bit restart function
    @Override
    public void restartFluentBit(
            SshConnection connection, String ip, int port, String user, String password) {

        String restartCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl restart "
                        + getFluentBitServiceName();

        try {
            // 1) If not enabled, cannot restart
            if (!isEnable(Agent.FLUENT_BIT, ip, port, user, password, connection)) {
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Fluent Bit is not enabled. Cannot restart.",
                        Agent.FLUENT_BIT);
            }

            // 2) Execute restart command
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
    public void enableTelegraf(
            SshConnection connection, String ip, int port, String user, String password) {

        String enableCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl enable --now "
                        + getTelegrafServiceName();

        AgentCommandResult enableRes;

        boolean isAlreadyEnabled;

        try {
            isAlreadyEnabled = isEnable(Agent.TELEGRAF, ip, port, user, password, connection);

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
    // telegraf disable function
    public void disableTelgraf(
            SshConnection connection, String ip, int port, String user, String password) {

        String disableCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl disable --now "
                        + getTelegrafServiceName();

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

    // telegraf restart function
    @Override
    public void restartTelegraf(
            SshConnection connection, String ip, int port, String user, String password) {

        String restartCommand =
                "echo '"
                        + password
                        + "' | sudo -S -p '' systemctl restart "
                        + getTelegrafServiceName();

        try {
            // 1) If not enabled, cannot restart
            if (!isEnable(Agent.TELEGRAF, ip, port, user, password, connection)) {
                throw new AgentStatusException(
                        requestInfo.getRequestId(),
                        "Telegraf is not enabled. Cannot restart.",
                        Agent.TELEGRAF);
            }

            // 2) Execute restart command
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
