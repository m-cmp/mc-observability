package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.model.agentHealth.SshConnection;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SshService {

    SshConnection getConnectionWithPrivateKey(
            String ip, int port, String user, Path privateKeyPath);

    SshConnection getConnectionWithPrivateKeyString(
            String ip, int port, String user, String privateKeyContent);

    String runCommandWithPrivateKey(
            String ip, int port, String user, Path privateKeyPath, String command);

    String runCommandWithPrivateKeyString(
            String ip, int port, String user, String privateKeyContent, String command);

    boolean isEnable(Agent agent, String ip, int port, String user, SshConnection sshConnection);

    void removeConnection(String ip, int port, String user);

    boolean existDirectory(SshConnection connection, String path);

    boolean checkDirectoryExistsFromRemote(SshConnection connection, String dirPath);

    boolean checkFluentBitDirectoryExistsFromRemote(SshConnection connection);

    boolean isExistTelegrafConfigDirectory(SshConnection connection);

    List<String> listDirectory(SshConnection connection, String directoryPath) throws IOException;

    String readFileContent(SshConnection connection, String filePath) throws IOException;

    void download(
            SshConnection connection,
            String remoteFilePath,
            Path localFilePath,
            String username,
            String host,
            int port,
            String privateKeyContent)
            throws IOException;

    boolean isExistFluentbitConfigDirectory(SshConnection connection);

    void enableFluentBit(SshConnection connection, String ip, int port, String user);

    void disableFluentBit(SshConnection connection, String ip, int port, String user);

    void restartFluentBit(SshConnection connection, String ip, int port, String user);

    void enableTelegraf(SshConnection connection, String ip, int port, String user);

    void disableTelegraf(SshConnection connection, String ip, int port, String user);

    void restartTelegraf(SshConnection connection, String ip, int port, String user);
}
