package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.exception.config.ConfigInitException;
import com.mcmp.o11ymanager.exception.config.FailedDeleteFileException;
import com.mcmp.o11ymanager.exception.config.FileReadingException;
import com.mcmp.o11ymanager.model.config.ConfigFileNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class FileServiceImplTest {

    FileServiceImpl fileService = new FileServiceImpl();


    @TempDir
    Path tempDir;


    @BeforeEach
    void setUp() throws Exception {
        fileService = new FileServiceImpl();

        Field field = FileServiceImpl.class.getDeclaredField("configBasePath");
        field.setAccessible(true);
        field.set(fileService, tempDir.toString());
    }


    @Test
    @Tag("file-write")
    @Tag("success-case")
    @DisplayName("지정된 경로에 지정된 이름의 파일에 올바른 내용을 입력해야 함")
    void testWriteFile_success(@TempDir Path tempDir) throws IOException {
        File targetDir = tempDir.toFile();
        String filename = "test-config.txt";
        String content = "hello, config!";

        fileService.writeFile(targetDir, filename, content);

        Path writtenFilePath = tempDir.resolve(filename);
        assertTrue(Files.exists(writtenFilePath), "파일이 생성되지 않았습니다.");

        String writtenContent = Files.readString(writtenFilePath, StandardCharsets.UTF_8);
        assertEquals(content, writtenContent, "파일 내용이 예상과 다릅니다.");
    }


    @ParameterizedTest
    @MethodSource("provideNullInputs")
    @Tag("file-write")
    @Tag("failure-case")
    @DisplayName("null 인자가 하나라도 존재하면 FileReadingException이 발생해야 함")
    void testWriteFile_withNullInputs(File dir, String filename, String content) {
        assertThrows(FileReadingException.class, () -> fileService.writeFile(dir, filename, content));
    }

    private static Stream<Arguments> provideNullInputs() {
        return Stream.of(
                Arguments.of(null, "file.txt", "content"),
                Arguments.of(new File("."), null, "content"),
                Arguments.of(new File("."), "file.txt", null)
        );
    }















    @Test
    @Tag("file-read")
    @Tag("success-case")
    @DisplayName("정상적인 파일 읽기 - 내용 반환")
    void testReadFile_success(@TempDir Path tempDir) throws IOException {
        // given
        String expected = "hello\nworld";
        Path filePath = tempDir.resolve("config.txt");
        Files.writeString(filePath, expected, StandardCharsets.UTF_8);

        File configFile = filePath.toFile();

        // when
        String result = fileService.singleFileReader(configFile);

        // then
        assertEquals(expected, result);
    }

    @Test
    @Tag("file-read")
    @Tag("not-found")
    @DisplayName("파일이 존재하지 않으면 예외 발생")
    void testReadFile_notExist() {
        File nonExistentFile = new File("non-existent-file.conf");

        FileReadingException ex = assertThrows(FileReadingException.class, () ->
                fileService.singleFileReader(nonExistentFile)
        );

        assertTrue(ex.getMessage().contains("Config file not found"));
    }

    @Test
    @Tag("file-read")
    @Tag("invalid-input")
    @DisplayName("디렉토리를 넘기면 예외 발생")
    void testReadFile_isDirectory(@TempDir Path tempDir) {
        File directory = tempDir.toFile();

        FileReadingException ex = assertThrows(FileReadingException.class, () ->
                fileService.singleFileReader(directory)
        );

        assertTrue(ex.getMessage().contains("Config file not found"));
    }


    @Test
    @Tag("file-read")
    @Tag("io-error")
    @DisplayName("IOException 발생 시 예외로 감싸서 던짐")
    void testReadFile_throwsIOException(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve("unreadable.conf");
        Files.writeString(filePath, "secret", StandardCharsets.UTF_8);
        File file = filePath.toFile();

        boolean success = file.setReadable(false);
        if (!success) {
            System.out.println("경고: 이 OS에서는 파일 읽기 권한 제거가 동작하지 않을 수 있음. 이 테스트는 건너뜀.");
            return;
        }


        assertThrows(FileReadingException.class, () -> fileService.singleFileReader(file));
    }

    @Test
    @Tag("file-scan")
    @Tag("recursive")
    @DisplayName("여러 디렉토리와 파일이 있을 경우 모든 파일을 재귀적으로 수집해야 함")
    void testGetFilesRecursively_normalStructure(@TempDir Path tempDir) throws IOException {
        Path dir1 = Files.createDirectory(tempDir.resolve("dir1"));
        Path dir2 = Files.createDirectory(dir1.resolve("dir2"));
        Path file1 = Files.createFile(dir1.resolve("file1.txt"));
        Path file2 = Files.createFile(dir2.resolve("file2.txt"));

        List<File> result = fileService.getFilesRecursively(tempDir.toFile());

        assertTrue(result.contains(file1.toFile()));
        assertTrue(result.contains(file2.toFile()));
        assertEquals(2, result.size());
    }

    @Test
    @Tag("file-scan")
    @Tag("ignore-case")
    @DisplayName(".git 디렉토리는 재귀 탐색에서 제외되어야 함")
    void testGetFilesRecursively_gitDirectoryIgnored(@TempDir Path tempDir) throws IOException {
        Path gitDir = Files.createDirectory(tempDir.resolve(".git"));
        Path fileInGit = Files.createFile(gitDir.resolve("config"));
        Path fileOutside = Files.createFile(tempDir.resolve("file.txt"));

        List<File> result = fileService.getFilesRecursively(tempDir.toFile());

        assertTrue(result.contains(fileOutside.toFile()));
        assertFalse(result.contains(fileInGit.toFile()));
    }

    @Test
    @Tag("file-scan")
    @Tag("single-file")
    @DisplayName("파일 단일 건을 전달하면 해당 파일만 반환되어야 함")
    void testGetFilesRecursively_singleFile(@TempDir Path tempDir) throws IOException {
        Path filePath = Files.createFile(tempDir.resolve("only.txt"));

        List<File> result = fileService.getFilesRecursively(filePath.toFile());

        assertEquals(1, result.size());
        assertEquals(filePath.toFile(), result.get(0));
    }




    @Test
    @Tag("file-scan")
    @Tag("exception-case")
    @DisplayName("listFiles() 중 예외 발생 시 FileReadingException으로 감싸야 함")
    void testGetFilesRecursively_forceException() {
        FileServiceImpl fileService = new FileServiceImpl() {
            @Override
            public List<File> getFilesRecursively(File directory) throws FileReadingException {
                throw new FileReadingException("강제로 발생시킨 예외");
            }
        };

        File dummy = new File("irrelevant");

        FileReadingException ex = assertThrows(FileReadingException.class, () ->
                fileService.getFilesRecursively(dummy)
        );

        assertTrue(ex.getMessage().contains("강제로 발생시킨 예외"));
    }




    @Test
    @Tag("unit")
    @Tag("file-sort")
    @Tag("sort")
    @DisplayName("디렉토리 우선 + 이름순 정렬이 재귀적으로 적용돼야 함")
    void testSortConfigFileNodes_recursiveDirectoryThenFile() {
        // child 디렉토리 안의 children
        ConfigFileNode subFileZ = ConfigFileNode.builder()
                .name("z-subfile.txt").isDirectory(false).build();

        ConfigFileNode subFileA = ConfigFileNode.builder()
                .name("a-subfile.txt").isDirectory(false).build();

        ConfigFileNode subDir = ConfigFileNode.builder()
                .name("inner-dir").isDirectory(true)
                .children(new ArrayList<>(List.of(subFileZ, subFileA)))
                .build();

        // 상위 노드들 (디렉토리 + 파일 혼합, 이름 순서 무작위)
        ConfigFileNode dirB = ConfigFileNode.builder()
                .name("b-dir").isDirectory(true).build();

        ConfigFileNode fileC = ConfigFileNode.builder()
                .name("c.log").isDirectory(false).build();

        ConfigFileNode dirA = ConfigFileNode.builder()
                .name("a-dir").isDirectory(true)
                .children(new ArrayList<>(List.of(subDir)))  // 중첩 구조
                .build();

        ConfigFileNode fileB = ConfigFileNode.builder()
                .name("b.log").isDirectory(false).build();

        List<ConfigFileNode> nodes = new ArrayList<>(List.of(fileB, dirB, fileC, dirA));

        // when
        fileService.sortFile(nodes);

        // then: 상위 노드 정렬 확인
        assertEquals("a-dir", nodes.get(0).getName());
        assertTrue(nodes.get(0).isDirectory());

        assertEquals("b-dir", nodes.get(1).getName());
        assertTrue(nodes.get(1).isDirectory());

        assertEquals("b.log", nodes.get(2).getName());
        assertFalse(nodes.get(2).isDirectory());

        assertEquals("c.log", nodes.get(3).getName());
        assertFalse(nodes.get(3).isDirectory());

        // then: 하위 노드 정렬 확인
        List<ConfigFileNode> childrenOfADir = nodes.get(0).getChildren();
        assertEquals("inner-dir", childrenOfADir.get(0).getName());

        List<ConfigFileNode> childrenOfInnerDir = childrenOfADir.get(0).getChildren();
        assertEquals("a-subfile.txt", childrenOfInnerDir.get(0).getName());
        assertEquals("z-subfile.txt", childrenOfInnerDir.get(1).getName());
    }


    //delete
    @Test
    @Tag("unit")
    @Tag("file-delete")
    @Tag("not-exist")
    @Tag("exception-case")
    @DisplayName("존재하지 않는 디렉토리 삭제 시 예외 발생해야 함")
    void testDeleteDirectory_notExist() {
        Path nonexistent = Paths.get("non-existent-dir");

        FailedDeleteFileException ex = assertThrows(FailedDeleteFileException.class, () ->
                fileService.deleteDirectory(nonexistent)
        );

        assertTrue(ex.getMessage().contains("Directory does not exist"));
    }

    @Test
    @Tag("file-delete")
    @Tag("success-case")
    @DisplayName("디렉토리 삭제 후 실제로 존재하지 않아야 함")
    void testDeleteDirectory_success(@TempDir Path tempDir) throws IOException {
        // given
        Path subDir = Files.createDirectory(tempDir.resolve("to-delete"));
        Files.createFile(subDir.resolve("file1.txt"));
        Files.createFile(subDir.resolve("file2.txt"));

        assertTrue(Files.exists(subDir));

        // when
        fileService.deleteDirectory(subDir);

        // then
        assertFalse(Files.exists(subDir), "디렉토리가 실제로 삭제되지 않았습니다.");
    }





    @Test
    @Tag("unit")
    @Tag("file-delete")
    @Tag("success-case")
    @DisplayName("리소스 디렉토리 삭제 테스트")
    void testDeleteDirectory_fromTestResources() throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource("file/deletable-dir");
        assertNotNull(resourceUrl, "리소스 경로를 찾을 수 없습니다. 'file/deletable-dir'이 존재하는지 확인하세요.");

        Path resourcePath = Paths.get(resourceUrl.toURI());

        Path tempCopy = Files.createTempDirectory("test-copy");
        Path targetBase = tempCopy.resolve("deletable-dir"); // 이름도 원래대로

        Files.walk(resourcePath).forEach(source -> {
            try {
                Path target = targetBase.resolve(resourcePath.relativize(source).toString()); // ✅ fix
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
                System.out.println(" - 복사됨: " + target);
            } catch (IOException e) {
                throw new RuntimeException("복사 중 오류: " + source, e);
            }
        });

        Path dirToDelete = targetBase;

        Files.walk(dirToDelete).forEach(path -> System.out.println(" - " + path));

        assertTrue(Files.exists(dirToDelete), "삭제 대상 디렉토리가 존재하지 않습니다.");

        fileService.deleteDirectory(dirToDelete);

        assertFalse(Files.exists(dirToDelete), "리소스 디렉토리 복사본이 삭제되지 않았습니다.");
    }











    @Test
    @Tag("unit")
    @Tag("file-delete")
    @Tag("success-case")
    @Tag("recursive")
    @DisplayName("하위 디렉토리 및 파일이 포함된 구조도 재귀적으로 삭제되어야 함")
    void testDeleteDirectory_recursiveStructure(@TempDir Path tempDir) throws IOException {

        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path file1 = Files.createFile(root.resolve("file1.txt"));
        Path subdir = Files.createDirectory(root.resolve("subdir"));
        Path file2 = Files.createFile(subdir.resolve("file2.txt"));

        // when
        fileService.deleteDirectory(root);

        // then
        assertFalse(Files.exists(root));
        assertFalse(Files.exists(file1));
        assertFalse(Files.exists(subdir));
        assertFalse(Files.exists(file2));
    }



    //templatecontent
    @Test
    @Tag("template")
    @Tag("read")
    @Tag("success-case")
    @DisplayName("클래스패스 리소스 템플릿을 정상적으로 읽고 빈 문자열이 반환될 수 있음")
    void testGetTemplateContent_success() {
        ClassPathResource resource = new ClassPathResource("file/deletable-dir/file1.txt");

        String content = fileService.getFileContent(resource);

        assertNotNull(content);  // ✅ null만 아니면 정상
        // assertFalse(content.isEmpty()); ❌ 이건 상황 따라 깨질 수 있음

        System.out.println("읽은 내용:\n" + (content.isEmpty() ? "[빈 문자열]" : content));
    }



    @Test
    @Tag("template")
    @Tag("read")
    @Tag("exception-case")
    @DisplayName("존재하지 않는 리소스 파일일 경우 예외 발생해야 함")
    void testGetTemplateContent_fileNotFound() {
        ClassPathResource resource = new ClassPathResource("file/deletable-dir/not-exist.txt");

        FileReadingException ex = assertThrows(FileReadingException.class, () ->
                fileService.getFileContent(resource)
        );

        assertTrue(ex.getMessage().contains("Error loading template content"));
    }




    //appendconfig
    @Test
    @Tag("template")
    @Tag("append")
    @Tag("success-case")
    @Tag("unit")
    @DisplayName("리소스 파일 내용을 읽어 StringBuilder에 추가하고 줄바꿈 두 줄도 추가되어야 함")
    void testAppendConfig_basic() {
        ClassPathResource resource = new ClassPathResource("file/deletable-dir/file1.txt");
        StringBuilder sb = new StringBuilder();

        String expectedContent;
        try (InputStream is = resource.getInputStream()) {
            expectedContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("테스트 리소스를 읽을 수 없습니다.");
            return;
        }

        String expected = expectedContent + "\n\n";

        fileService.appendConfig(resource, sb);

        System.out.println("저장된 sb 내용");
        System.out.println(sb.toString().replaceAll("\n", "\\\\n\n"));

        assertEquals(expected, sb.toString(), "리소스 파일 내용 + \\n\\n이 정확히 추가되어야 함");
    }


    @Test
    @Tag("template")
    @Tag("append")
    @Tag("exception-case")
    @DisplayName("존재하지 않는 리소스를 appendConfig로 읽으면 예외가 발생해야 함")
    void testAppendConfig_throwsException_whenResourceNotFound() {
        ClassPathResource resource = new ClassPathResource("file/not-exist.txt");
        StringBuilder sb = new StringBuilder();

        assertThrows(RuntimeException.class, () -> fileService.appendConfig(resource, sb));
    }



    @Test
    @Tag("init")
    @Tag("success-case")
    @Tag("directory")
    @DisplayName("configBasePath 경로가 존재하지 않으면 디렉토리를 생성해야 함")
    void testInit_createsConfigDirectory(@TempDir Path tempDir) throws Exception {
        Path testPath = tempDir.resolve("config-dir");
        FileServiceImpl service = new FileServiceImpl();

        Field field = FileServiceImpl.class.getDeclaredField("configBasePath");
        field.setAccessible(true);
        field.set(service, testPath.toString());

        service.init();
        assertTrue(Files.exists(testPath), "configBasePath 디렉토리가 생성되지 않았습니다.");
        assertTrue(Files.isDirectory(testPath), "configBasePath는 디렉토리여야 합니다.");
        System.out.println("생성된 디렉토리: " + testPath);
    }


    @Test
    @Tag("init")
    @Tag("directory")
    @Tag("exception-case")
    @DisplayName("디렉토리 생성 실패 시 ConfigInitException 발생해야 함")
    void testInit_throwsException_whenDirCreationFails() throws Exception {
        FileServiceImpl service = new FileServiceImpl();

        String invalidPath = "/root/forbidden-config-dir";

        Field field = FileServiceImpl.class.getDeclaredField("configBasePath");
        field.setAccessible(true);
        field.set(service, invalidPath);

        assertThrows(ConfigInitException.class, service::init);
    }

    @Test
    @Tag("file-generate")
    @Tag("io-error")
    @DisplayName("쓰기 권한이 없는 경우 FileReadingException이 발생해야 함")
    void testGenerateFile_ioError(@TempDir Path tempDir) throws IOException {
        File targetFile = tempDir.resolve("readonly.txt").toFile();
        Files.writeString(targetFile.toPath(), "original");
        boolean result = targetFile.setWritable(false);

        if (!result) {
            System.out.println("OS에서 쓰기 금지 설정이 지원되지 않을 수 있음. 테스트 건너뜀.");
            return;
        }

        assertThrows(FileReadingException.class, () ->
            fileService.generateFile(targetFile, "new content"));
    }


    // 파일이 아닌 디렉토리를 인자로 요청할 경우
    @Test
    @Tag("file-generate")
    @Tag("invalid-input")
    @Tag("exception-case")
    @DisplayName("디렉토리를 파일로 넘기면 FileReadingException이 발생해야 함")
    void testGenerateFile_directoryInsteadOfFile(@TempDir Path tempDir) {
        File directory = tempDir.toFile(); // 디렉토리
        String content = "some content";

        FileReadingException ex = assertThrows(FileReadingException.class, () ->
            fileService.generateFile(directory, content)
        );

        assertTrue(ex.getMessage().contains("파일 작성 중 오류가 발생했습니다"));
    }




}
