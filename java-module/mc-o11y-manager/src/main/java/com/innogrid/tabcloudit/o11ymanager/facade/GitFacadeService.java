package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.exception.agent.AgentConfigNotFoundException;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FileReadingException;
import com.innogrid.tabcloudit.o11ymanager.exception.git.*;
import com.innogrid.tabcloudit.o11ymanager.exception.host.AgentFailureException;
import com.innogrid.tabcloudit.o11ymanager.exception.host.BadRequestException;
import com.innogrid.tabcloudit.o11ymanager.global.definition.ConfigDefinition;
import com.innogrid.tabcloudit.o11ymanager.model.config.ConfigFileNode;
import com.innogrid.tabcloudit.o11ymanager.model.config.GitCommit;
import com.innogrid.tabcloudit.o11ymanager.service.domain.HostDomainService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.FileService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.GitService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.StatusService;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitFacadeService {

  private final GitService gitService;
  private final FileService fileService;
  private final HostDomainService hostDomainService;

  private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks = new ConcurrentHashMap<>();
  private final StatusService statusService;

  @Value("${config.base-path:./config}")
  private String configBasePath;


  public ReentrantLock getRepositoryLock(String uuid, Agent agent) {
    String lockKey = uuid + "-" + agent.name();
    return repositoryLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
  }

  public String getConfigGitHash(String requestId, String uuid, Agent agent) {
    ReentrantLock lock = getRepositoryLock(uuid, agent);

    try {
      lock.lock();

      Path hostConfigDir = Path.of(configBasePath, uuid);
      String subPath = switch (agent) {
        case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
        case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
        default -> throw new FileReadingException("Unsupported agent: " + agent);
      };
      Path agentConfigDir = hostConfigDir.resolve(subPath);
      if (!Files.exists(agentConfigDir) || !Files.isDirectory(agentConfigDir)) {
        throw new FileReadingException("Config directory not found: " + agentConfigDir);
      }

      try {
        Git git = gitService.getGit(agentConfigDir.toFile());
        return gitService.getHashName(git);
      } catch (GitFileOpenException | GitHashNotFoundException e) {
        statusService.resetHostAgentTaskStatus(requestId, uuid, agent);
        throw new BadRequestException(requestId, uuid, agent, "Config 폴더의 Git hash 값을 가져올 수 없습니다!: "
            + e.getMessage());
      }

    } finally {
      lock.unlock();
    }
  }

  public void revertLastCommit(String requestId, String uuid, Agent agent) {
    ReentrantLock lock = getRepositoryLock(uuid, agent);

    try {
      lock.lock();

      Path hostConfigDir = Path.of(configBasePath, uuid);
      String subPath = switch (agent) {
        case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
        case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
        default -> throw new FileReadingException("Unsupported agent: " + agent);
      };
      Path agentConfigDir = hostConfigDir.resolve(subPath);
      //GIt 서비스 리팩토링 완료
      try {
        Git git = gitService.getGit(agentConfigDir.toFile());
        gitService.revertLastCommit(git);
        log.info("Successfully reverted the last commit for agent: {} in host: {}", agent, uuid);
      } catch (GitFileOpenException e) {
        log.error("Failed to open Git repository", e);
        statusService.resetHostAgentTaskStatus(requestId, uuid, agent);
        throw new BadRequestException(requestId, uuid, agent,
            "Failed to open Git repository: " + e.getMessage());
      } catch (GitRevertException e) {
        log.error("Failed to revert Git repository", e);
        statusService.resetHostAgentTaskStatus(requestId, uuid, agent);
        throw new BadRequestException(requestId, uuid, agent,
            "Failed to revert Git repository: " + e.getMessage());
      }

    } finally {
      lock.unlock();
    }
  }

  public String getFileContentOfCommitHash(String requestId, String uuid, Agent agent,
      String commitHash, String filePath) throws AgentFailureException {
    ReentrantLock lock = getRepositoryLock(uuid, agent);

    try {
      lock.lock();

      Path hostConfigDir = Path.of(configBasePath, uuid);
      String subPath = switch (agent) {
        case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
        case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
        default -> throw new FileReadingException("Unsupported agent: " + agent);
      };
      Path agentConfigDir = hostConfigDir.resolve(subPath);

      log.info("[GitFacadeService] agentConfigDir: {}", agentConfigDir);
      // /home/kimsua/ideaProjects/o11y/o11y-manager/config/3/telegraf
      log.info("[GitFacadeService] Input filePath: {}", filePath);
      // /home/kimsua/ideaProjects/o11y/o11y-manager/config/3/telegraf

      log.info("[GitFacadeService] commitHash: {}", commitHash);
      // 리퀘스트 바디에 넣은 커밋 해시값과 일치 (문제 없음)

      try {
        Git git = gitService.getGit(agentConfigDir.toFile());
        Repository repo = git.getRepository();
        log.info("[GitFacadeService] git.getRepository().getWorkTree(): {}", repo.getWorkTree());
        // 레포티지토리 : Repository[/home/kimsua/Ideaprojects/o11y/o11y-manager/config/3/telegraf/.git]

        // 경로 확인: Git 기준에서 상대 경로로 변환 (테스트용)
        Path repoRoot = repo.getWorkTree().toPath();
        Path originalPath = agentConfigDir.resolve(filePath);
        String gitFilePath = null;
        if (originalPath.startsWith(repoRoot)) {
          Path relative = repoRoot.relativize(originalPath);
          gitFilePath = relative.toString().replace("\\", "/");
          log.info("[GitFacadeService] Resolved relative file path: {}", relative);
          // originPath = /home~ 에서부터 telegraf.conf 까지나옴
        } else {
          log.warn(
              "[GitFacadeService] filePath is not under repository root! filePath={}, repoRoot={}",
              filePath, repoRoot);
          // rootPath는 /telegraf 디렉토리까지만 나옴
        }

        return gitService.getCommitContents(git, commitHash, gitFilePath);
        // 최종 file path는 .telegraf.conf까지 나옴
      } catch (GitFileOpenException | GitCommitContentsException e) {
        log.error("[GitFacadeService] Failed to read commit content: {}", e.getMessage(), e);
        statusService.resetHostAgentTaskStatus(requestId, uuid, agent);
        throw new BadRequestException(requestId, uuid, agent,
            "Failed to get config by git hash: " + commitHash);
      }

    } finally {
      lock.unlock();
    }
  }


  public List<GitCommit> getConfigHistory(String requestId, String uuid, Agent agent,
      Integer page, Integer size) {
    ReentrantLock lock = getRepositoryLock(uuid, agent);

    try {
      lock.lock();

      Path hostConfigDir = Path.of(configBasePath, uuid);
      String subPath = switch (agent) {
        case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
        case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
        default -> throw new FileReadingException("Unsupported agent: " + agent);
      };
      Path agentConfigDir = hostConfigDir.resolve(subPath);

      try {

        Git git = gitService.getGit(agentConfigDir.toFile());
        Iterable<RevCommit> commits = gitService.history(git, page, size);

        List<GitCommit> history = new ArrayList<>();
        for (RevCommit commit : commits) {
          history.add(GitCommit.builder()
              .commitHash(commit.getName())
              .message(commit.getFullMessage())
              .timestamp(new Timestamp(commit.getCommitTime() * 1000L))
              .build());
        }

        return history;

      } catch (GitFileOpenException | GitHistoryNotFoundException e) {
        log.error("Failed to get config history", e);
        statusService.resetHostAgentTaskStatus(requestId, uuid, agent);
        throw new BadRequestException(requestId, uuid, agent, "Config 변경 내역을 가져올 수 없습니다!: "
            + e.getMessage());
      }

    } finally {
      lock.unlock();
    }
  }

  public List<ConfigFileNode> getConfigFileList(String requestId,
      String uuid, String commitHash, Agent agent) {

    ReentrantLock lock = getRepositoryLock(uuid, agent);

    try {
      lock.lock();

      Path hostConfigDir = Path.of(configBasePath, uuid);
      String subPath = switch (agent) {
        case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
        case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
        default -> throw new FileReadingException("Unsupported agent: " + agent);
      };
      Path agentConfigDir = hostConfigDir.resolve(subPath);

      File baseDir = agentConfigDir.toFile();

      List<File> fileList = fileService.getFilesRecursively(baseDir);

      Map<String, ConfigFileNode> nodeMap = new HashMap<>();
      List<ConfigFileNode> rootNodes = new ArrayList<>();

      for (File f : fileList) {
        Path relPath = baseDir.toPath().relativize(f.toPath());
        String path = relPath.toString().replace(File.separator, "/");

        ConfigFileNode node = new ConfigFileNode();
        node.setPath(path);
        node.setName(getNameFromPath(path));
        node.setDirectory(f.isDirectory());
        node.setChildren(new ArrayList<>());

        nodeMap.put(path, node);
      }

      for (ConfigFileNode node : nodeMap.values()) {
        String parentPath = getParentPath(node.getPath());
        if (parentPath != null && nodeMap.containsKey(parentPath)) {
          nodeMap.get(parentPath).getChildren().add(node);
        } else {
          rootNodes.add(node);
        }
      }

      return fileService.sortFile(rootNodes);

    } catch (Exception e) {
      throw new AgentConfigNotFoundException(requestId, uuid, agent);
    } finally {
      lock.unlock();
    }
  }


  private String getNameFromPath(String path) {
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash < path.length() - 1) {
      return path.substring(lastSlash + 1);
    }
    return path;
  }

  private String getParentPath(String path) {
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash > 0) {
      return path.substring(0, lastSlash);
    }
    return null;
  }





}
