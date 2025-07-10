package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.AgentCommandResult;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SshService {

  SshConnection getConnection(String ip, int port, String user, String password);

  void updateConnection(String ip, int port, String user, String password);

  String runCommand(String ip, int port, String user, String password, String command);

  AgentCommandResult runCommandWithResult(String ip, int port, String password, String user,
      String command);

  boolean isEnable(Agent agent, String ip, int port, String user, String password,
      SshConnection sshConnection);

  void removeConnection(String ip, int port, String user);

  boolean existDirectory(SshConnection connection, String path);

  boolean checkDirectoryExistsFromRemote(SshConnection connection, String dirPath);

  boolean checkFluentBitDirectoryExistsFromRemote(SshConnection connection);

  boolean isExistTelegrafConfigDirectory(SshConnection connection);


  List<String> listDirectory(SshConnection connection, String directoryPath) throws IOException;

  String readFileContent(SshConnection connection, String filePath) throws IOException;

  void download(SshConnection connection, String remoteFilePath, Path localFilePath,
      String username, String host, int port, String password) throws IOException;

  boolean isExistFluentbitConfigDirectory(SshConnection connection);

  void enableFluentBit(SshConnection connection, String ip, int port, String user,
      String password);


  void disableFluentBit(SshConnection connection, String ip, int port, String user,
      String password);

  void restartFluentBit(SshConnection connection, String ip, int port, String user,
      String password);

  void enableTelegraf(SshConnection connection, String ip, int port, String user,
      String password);

  void disableTelgraf(SshConnection connection, String ip, int port, String user,
      String password);

  void restartTelegraf(SshConnection connection, String ip, int port, String user,
      String password);
}
