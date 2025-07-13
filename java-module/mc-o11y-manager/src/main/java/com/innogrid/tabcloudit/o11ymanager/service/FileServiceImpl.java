package com.innogrid.tabcloudit.o11ymanager.service;

import com.innogrid.tabcloudit.o11ymanager.exception.config.ConfigInitException;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FailedDeleteFileException;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FileReadingException;
import com.innogrid.tabcloudit.o11ymanager.model.config.ConfigFileNode;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.FileService;
import jakarta.annotation.PostConstruct;
import java.nio.file.attribute.DosFileAttributeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

  @Value("${config.base-path:./config}")
  private String configBasePath;

  @Override
  public String singleFileReader(File file) throws FileReadingException {
    if (!file.exists() || !file.isFile()) {
      String errMsg = "Config file not found: " + file.getAbsolutePath();
      log.error(errMsg);
      throw new FileReadingException(errMsg);
    }

    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
    ) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      String errMsg = "Failed to read config file: " + file.getAbsolutePath();
      log.error(errMsg, e);
      throw new FileReadingException(errMsg);
    }
  }

  //파일 수집
  @Override
  public List<File> getFilesRecursively(File dir) throws FileReadingException {
    List<File> files = new ArrayList<>();
    try {
      if (dir.isDirectory() && !dir.getName().equals(".git")) {
        File[] entries = dir.listFiles();
        if (entries != null) {
          for (File entry : entries) {
            files.add(entry);
            if (entry.isDirectory()) {
              files.addAll(getFilesRecursively(entry));
            }
          }
        }
      } else if (dir.isFile()) {
        files.add(dir);
      }
    } catch (Exception e) {
      String errMsg = "Failed to get files recursively: " + dir.getAbsolutePath();
      log.error(errMsg, e);
      throw new FileReadingException(errMsg);
    }

    return files;
  }

  @Override
  public List<ConfigFileNode> sortFile(List<ConfigFileNode> nodes) {
    nodes.sort(Comparator.comparing(ConfigFileNode::isDirectory).reversed()
        .thenComparing(ConfigFileNode::getName));

    for (ConfigFileNode node : nodes) {
      if (node.isDirectory() && node.getChildren() != null) {
        sortFile(node.getChildren());
      }
    }

    return nodes;
  }

  @Override
  public String getClassResourceContent(ClassPathResource classPathResource) {
    try (
        InputStream is = new BufferedInputStream(classPathResource.getInputStream());
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      return br.lines()
          .collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  //파일 이름, 내용 write
  @Override
  public void writeFile(File directory, String fileName, String content) throws FileReadingException {
    if (directory == null || fileName == null || content == null) {
      throw new FileReadingException("파일 작성에 필요한 인자가 null입니다.");
    }

    try {
      Path filePath = new File(directory, fileName).toPath();
      Files.writeString(filePath, content);
    } catch (IOException e) {
      throw new FileReadingException("파일 작성 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  //파일, 내용 write
  @Override
  public void generateFile(File file, String content) throws FileReadingException {
    if (file == null || content == null) {
      throw new FileReadingException("파일 작성에 필요한 인자가 null입니다.");
    }

    try {
      Files.writeString(file.toPath(), content);
    } catch (IOException e) {
      throw new FileReadingException("파일 작성 중 오류가 발생했습니다: " + e.getMessage());
    }
  }


  @Override
  public void deleteDirectory(Path dir) throws FailedDeleteFileException {
    if (!Files.exists(dir)) {
      log.warn("삭제할 디렉터리가 없습니다: {}", dir);
      throw new FailedDeleteFileException("Directory does not exist: " + dir);
    }
    try (var paths = Files.walk(dir)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
              log.debug("Deleted: {}", path);
            } catch (IOException e) {
              String errMsg = "Failed deleted file: " + path;
              log.error(errMsg, e);
              throw new FailedDeleteFileException(errMsg);
            }
          });
    } catch (IOException e) {
      throw new FailedDeleteFileException("디렉터리 탐색 중 오류 발생: " + dir);
    }
  }

  //config 디렉토리 삭제, 호스트 삭제 시 호출
  @Override
  public void deleteDirectoryByHostId(String uuid) {
    Path dir = Path.of(configBasePath, uuid);

    if (!Files.exists(dir)) {
      log.warn("삭제할 디렉터리가 없습니다: {}", dir);
      throw new FailedDeleteFileException("Directory does not exist: " + dir);
    }
    try (var paths = Files.walk(dir)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              // 윈도우인 경우 권한 제거
              DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
              if (view != null) {
                view.setReadOnly(false);
              }

              Files.delete(path);
              log.debug("Deleted: {}", path);
            } catch (IOException e) {
              String errMsg = "Failed deleted file: " + path;
              log.error(errMsg, e);
              throw new FailedDeleteFileException(errMsg);
            }
          });
    } catch (IOException e) {
      throw new FailedDeleteFileException("디렉터리 탐색 중 오류 발생: " + dir);
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (var paths = Files.walk(path)) {
        paths
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                  try {
                    DosFileAttributeView view = Files.getFileAttributeView(p, DosFileAttributeView.class);
                    if (view != null) {
                      view.setReadOnly(false);
                    }
                    Files.delete(p);
                    log.debug("Deleted: {}", p);
                  } catch (IOException e) {
                    String errMsg = "Failed deleted file: " + p;
                    log.error(errMsg, e);
                    throw new FailedDeleteFileException(errMsg);
                  }
                });
      }
    } else {
      DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
      if (view != null) {
        view.setReadOnly(false);
      }
      Files.delete(path);
      log.debug("Deleted: {}", path);
    }
  }

  @Override
  public void deleteDirectoryExceptGitByHostId(String uuid) {
    Path dir = Path.of(configBasePath, uuid);
    if (!Files.exists(dir)) {
      return;
    }

    try (var paths = Files.walk(dir, 1)) {
      paths
              .filter(path -> !path.equals(dir))
              .filter(path -> !path.getFileName().toString().equals(".git"))
              .forEach(path -> {
                try {
                  deleteRecursively(path);
                } catch (IOException e) {
                  String errMsg = "Failed to delete: " + path;
                  log.error(errMsg, e);
                  throw new FailedDeleteFileException(errMsg);
                }
              });
    } catch (IOException e) {
      throw new FailedDeleteFileException("디렉터리 탐색 중 오류 발생: " + dir);
    }
  }

  //템플릿 리소스 로딩
  @Override
  public String getFileContent(ClassPathResource classPathResource) throws FileReadingException {
    StringBuilder sb = new StringBuilder();

    try (InputStream is = classPathResource.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
    } catch (IOException e) {
      String errMsg = "Error loading content: " + classPathResource.getPath();
      log.error(errMsg, e);
      throw new FileReadingException(errMsg);
    }

    return sb.toString();
  }


  //content 추가
  @Override
  public void appendConfig(ClassPathResource classPathResource, StringBuilder sb) {
    String content = getClassResourceContent(classPathResource);
    sb.append(content).append("\n\n");
  }


  //configBasePath 경로 하위 config 디렉토리 초기화 (Host의 id값 이름 폴더, 그 아래 fluent-bit, telegraf, telegraf.d 폴더)
  @Override
  @PostConstruct
  public void init() {
    File baseDir = new File(configBasePath);
    if (!baseDir.exists() && !baseDir.mkdirs()) {
      log.error("Failed to create config base directory: {}", configBasePath);
      throw new ConfigInitException(UUID.randomUUID().toString(), configBasePath);
    }
  }


  // 폴더 생성
  @Override
  public Path createDirectory(Path path) {
    try {
      return Files.createDirectories(path);
    } catch (IOException e) {
      // TODO 추후 수정
      throw new RuntimeException(e);
    }
  }

}
