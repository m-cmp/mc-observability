package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.exception.config.FailedDeleteFileException;
import com.mcmp.o11ymanager.manager.exception.config.FileReadingException;
import com.mcmp.o11ymanager.manager.model.config.ConfigFileNode;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface FileService {


    String singleFileReader(File configFile) throws FileReadingException;

    void deleteDirectory(Path dir) throws FailedDeleteFileException;

    List<File> getFilesRecursively(File directory) throws FileReadingException;

    List<ConfigFileNode> sortFile(List<ConfigFileNode> nodes);

    String getClassResourceContent(ClassPathResource classPathResource);

    void writeFile(File agentConfigDir, String configFilename, String configContent) throws FileReadingException;

    void deleteDirectoryByHostId(String uuid);

    String getFileContent(ClassPathResource classPathResource) throws FileReadingException;

    void appendConfig(ClassPathResource classPathResource, StringBuilder sb);

    void init();

    Path createDirectory(Path path);

    void generateFile(File file, String content) throws FileReadingException;

}
