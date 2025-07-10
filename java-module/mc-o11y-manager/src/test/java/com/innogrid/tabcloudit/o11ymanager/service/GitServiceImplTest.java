package com.innogrid.tabcloudit.o11ymanager.service;

import com.innogrid.tabcloudit.o11ymanager.exception.git.*;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.GitService;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {GitServiceImpl.class, MethodValidationPostProcessor.class})
class GitServiceImplTest {

    @Autowired
    private GitService gitService;

    @TempDir
    Path tempDir;

    @Test
    @Tag("init")
    @DisplayName("디렉토리가 존재할 때 Git 초기화가 성공해야 함")
    void init_WhenDirectoryExists_ShouldInitializeGit() {
        // given
        File directory = tempDir.toFile();
        assertTrue(directory.exists(), "테스트 디렉토리가 존재해야 합니다");

        // when
        gitService.init(directory);

        // then
        File gitDir = new File(directory, ".git");
        assertTrue(gitDir.exists(), "Git 초기화 후 .git 디렉토리가 생성되어야 합니다");
        assertTrue(gitDir.isDirectory(), ".git은 디렉토리여야 합니다");
    }

    @Test
    @Tag("init")
    @DisplayName("디렉토리가 존재하지 않을 때 디렉토리 생성 후 Git 초기화가 성공해야 함")
    void init_WhenDirectoryDoesNotExist_ShouldCreateDirectoryAndInitializeGit() {
        // given
        File nonExistentDir = new File(tempDir.toFile(), "non-existent");
        assertFalse(nonExistentDir.exists(), "테스트를 위한 디렉토리가 존재하지 않아야 합니다");

        // when
        gitService.init(nonExistentDir);

        // then
        assertTrue(nonExistentDir.exists(), "디렉토리가 생성되어야 합니다");
        File gitDir = new File(nonExistentDir, ".git");
        assertTrue(gitDir.exists(), "Git 초기화 후 .git 디렉토리가 생성되어야 합니다");
    }

    @Test
    @Tag("init")
    @DisplayName("대상이 디렉토리가 아닌 파일일 때 예외가 발생해야 함")
    void init_WhenDirectoryIsFile_ShouldThrowException() throws IOException {
        // given
        File file = new File(tempDir.toFile(), "test-file.txt");
        Files.createFile(file.toPath());
        assertTrue(file.exists() && file.isFile(), "테스트 파일이 생성되어야 합니다");

        // when, then
        assertThrows(Exception.class, () -> {
            gitService.init(file);
        });
    }

    @Test
    @Tag("init")
    @DisplayName(".git 디렉토리가 이미 존재할 때 Git 재초기화가 성공해야 함")
    void init_WhenGitDirectoryAlreadyExists_ShouldReInitialize() throws IOException {
        // given
        File directory = tempDir.toFile();

        // 먼저 Git 초기화
        gitService.init(directory);

        File gitDir = new File(directory, ".git");
        assertTrue(gitDir.exists(), "첫 번째 초기화 후 .git 디렉토리가 존재해야 합니다");

        // 검증용 파일 생성하여 나중에 여전히 존재하는지 확인
        File markerFile = new File(gitDir, "test-marker.txt");
        Files.createFile(markerFile.toPath());

        // when - 다시 초기화
        gitService.init(directory);

        // then
        assertTrue(gitDir.exists(), "재초기화 후에도 .git 디렉토리가 존재해야 합니다");
        // 기존 .git 디렉토리의 내용이 유지되는지 확인
        assertTrue(markerFile.exists(), "재초기화 후에도 기존 .git 디렉토리 내용이 유지되어야 합니다");
    }


    @Test
    @Tag("getGit")
    @DisplayName("디렉토리가 존재하고 git 저장소일 때 Git 객체를 반환해야 함")
    void getGit_WhenDirectoryExistsAndIsGitRepo_ShouldReturnGitObject() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // when
        Git git = gitService.getGit(directory);

        // then
        assertNotNull(git, "Git 객체가 반환되어야 합니다");
        assertTrue(git.getRepository().getDirectory().exists(), "유효한 Git 저장소여야 합니다");
        git.close(); // 리소스 해제
    }

    @Test
    @Tag("getGit")
    @DisplayName("디렉토리가 존재하지만 git 저장소가 아닐 때 GitFileOpenException이 발생해야 함")
    void getGit_WhenDirectoryExistsButNotGitRepo_ShouldThrowGitFileOpenException() {
        // given
        File directory = tempDir.toFile();
        assertTrue(directory.exists(), "테스트 디렉토리가 존재해야 합니다");

        File gitDir = new File(directory, ".git");
        assertFalse(gitDir.exists(), "Git 디렉토리가 존재하지 않아야 합니다");

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            gitService.getGit(directory);
        }, "Git 저장소가 아닌 경우 GitFileOpenException이 발생해야 합니다");
    }

    @Test
    @Tag("getGit")
    @DisplayName("디렉토리가 존재하지 않을 때 GitFileOpenException이 발생해야 함")
    void getGit_WhenDirectoryDoesNotExist_ShouldThrowGitFileOpenException() {
        // given
        File nonExistentDir = new File(tempDir.toFile(), "non-existent");
        assertFalse(nonExistentDir.exists(), "테스트를 위한 디렉토리가 존재하지 않아야 합니다");

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            gitService.getGit(nonExistentDir);
        }, "존재하지 않는 디렉토리에 대해 GitFileOpenException이 발생해야 합니다");
    }


    @Test
    @Tag("getRepository")
    @DisplayName("유효한 Git 객체가 주어졌을 때 Repository 객체를 반환해야 함")
    void getRepository_WhenValidGitObject_ShouldReturnRepository() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);
        Git git = gitService.getGit(directory);

        // when
        Repository repository = gitService.getRepository(git);

        // then
        assertNotNull(repository, "Repository 객체가 반환되어야 합니다");
        assertEquals(new File(directory, ".git"), repository.getDirectory(), "Repository는 .git 디렉토리를 가리켜야 합니다");

        // 리소스 해제
        git.close();
    }

    @Test
    @Tag("getRepository")
    @DisplayName("null Git 객체가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getRepository_WhenNullGitObject_ShouldThrowConstraintViolationException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.getRepository(null);
        }, "null Git 객체로부터 Repository를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("getObjectId")
    @DisplayName("유효한 Git 객체와 유효한, HEAD 해시값이 주어졌을 때 ObjectId를 반환해야 함")
    void getObjectId_WhenValidGitAndHeadHash_ShouldReturnObjectId() throws Exception {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 커밋을 생성하여 HEAD 레퍼런스를 만듭니다
        File testFile = new File(directory, "test.txt");
        Files.writeString(testFile.toPath(), "Test content");

        try (Git git = gitService.getGit(directory)) {
            gitService.commit(git, "test.txt", "Initial commit", "Test Author", "test@example.com");

            // when
            ObjectId objectId = gitService.getObjectId(git, "HEAD");

            // then
            assertNotNull(objectId, "ObjectId가 반환되어야 합니다");
            assertFalse(objectId.equals(ObjectId.zeroId()), "반환된 ObjectId는 zeroId가 아니어야 합니다");
        }
    }

    @Test
    @Tag("getObjectId")
    @DisplayName("유효한 Git 객체와 존재하지 않는 해시값이 주어졌을 때 null을 반환해야 함")
    void getObjectId_WhenValidGitAndNonExistentHash_ShouldReturnNull() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            // when
            ObjectId objectId = gitService.getObjectId(git, "non-existent-branch");

            // then
            assertNull(objectId, "존재하지 않는 해시에 대해 null을 반환해야 합니다");
        }
    }

    @Test
    @Tag("getObjectId")
    @DisplayName("null Git 객체가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getObjectId_WhenNullGit_ShouldThrowConstraintViolationException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.getObjectId(null, "HEAD");
        }, "null Git 객체로부터 ObjectId를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("getObjectId")
    @DisplayName("유효한 Git 객체와 null 해시값이 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getObjectId_WhenValidGitAndNullHash_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.getObjectId(git, null);
            }, "null 해시 문자열로부터 ObjectId를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getHashName")
    @DisplayName("커밋이 있는 git 저장소에서 최신 커밋의 해시를 반환해야 함")
    void getHashName_WhenRepoHasCommits_ShouldReturnLatestCommitHash() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        File testFile = new File(directory, "test.txt");
        Files.writeString(testFile.toPath(), "Test content");

        try (Git git = gitService.getGit(directory)) {
            gitService.commit(git, "test.txt", "Initial commit", "Test Author", "test@example.com");

            // 커밋된 해시 가져오기 (비교용)
            RevCommit commit = git.log().call().iterator().next();
            String expectedHash = commit.getName();


            // when
            String actualHash = gitService.getHashName(git);

            // then
            assertNotNull(actualHash, "해시값이 반환되어야 합니다");
            assertEquals(expectedHash, actualHash, "반환된 해시값이 최신 커밋의 해시값과 일치해야 합니다");
        } catch (GitAPIException e) {
            fail("테스트 셋업 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @Tag("getHashName")
    @DisplayName("커밋이 없는 git 저장소에서 GitHashNotFoundException이 발생해야 함")
    void getHashName_WhenRepoHasNoCommits_ShouldThrowGitHashNotFoundException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);
        Git git = gitService.getGit(directory);

        // when, then
        assertThrows(GitHashNotFoundException.class, () -> {
            gitService.getHashName(git);
        }, "커밋이 없는 저장소에서는 GitHashNotFoundException이 발생해야 합니다");
    }

    @Test
    @Tag("getHashName")
    @DisplayName("git 저장소가 아닌 디렉토리에서 GitFileOpenException이 발생해야 함")
    void getHashName_WhenDirectoryIsNotGitRepo_ShouldThrowGitFileOpenException() {
        // given
        File directory = tempDir.toFile();
        assertTrue(directory.exists(), "테스트 디렉토리가 존재해야 합니다");

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.getHashName(git);
        }, "Git 저장소가 아닌 디렉토리에서는 GitFileOpenException이 발생해야 합니다");
    }

    @Test
    @Tag("getHashName")
    @DisplayName("존재하지 않는 디렉토리에서 GitFileOpenException이 발생해야 함")
    void getHashName_WhenDirectoryDoesNotExist_ShouldThrowGitFileOpenException() {
        // given
        File nonExistentDir = new File(tempDir.toFile(), "non-existent");
        assertFalse(nonExistentDir.exists(), "테스트를 위한 디렉토리가 존재하지 않아야 합니다");

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            Git git = gitService.getGit(nonExistentDir);
            gitService.getHashName(git);
        }, "존재하지 않는 디렉토리에서는 GitFileOpenException이 발생해야 합니다");
    }


    @Test
    @Tag("getRevTree")
    @DisplayName("유효한 Repository와 ObjectId가 주어졌을 때 RevTree를 반환해야 함")
    void getRevTree_WhenValidRepoAndObjectId_ShouldReturnRevTree() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        File testFile = new File(directory, "test.txt");
        Files.writeString(testFile.toPath(), "Test content");

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, "test.txt", "Initial commit", "Test Author", "test@example.com");

            // 커밋의 ObjectId 얻기
            Repository repo = gitService.getRepository(git);
            ObjectId objectId = repo.resolve("HEAD");

            // when
            RevTree revTree = gitService.getRevTree(repo, objectId);

            // then
            assertNotNull(revTree, "RevTree 객체가 반환되어야 합니다");
            assertNotEquals(0, revTree.getId().getName().length(), "유효한 RevTree ID가 있어야 합니다");
        }
    }

    @Test
    @Tag("getRevTree")
    @DisplayName("null Repository가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getRevTree_WhenNullRepo_ShouldThrowConstraintViolationException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            File testFile = new File(directory, "test.txt");
            Files.writeString(testFile.toPath(), "Test content");
            gitService.commit(git, "test.txt", "Initial commit", "Test Author", "test@example.com");

            // ObjectId 얻기
            ObjectId objectId = git.getRepository().resolve("HEAD");

            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.getRevTree(null, objectId);
            }, "null Repository로부터 RevTree를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getRevTree")
    @DisplayName("null ObjectId가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getRevTree_WhenNullObjectId_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            Repository repo = gitService.getRepository(git);

            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.getRevTree(repo, null);
            }, "null ObjectId로부터 RevTree를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getRevTree")
    @DisplayName("유효하지 않은 ObjectId가 주어졌을 때 RuntimeException이 발생해야 함")
    void getRevTree_WhenInvalidObjectId_ShouldThrowRuntimeException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            Repository repo = gitService.getRepository(git);

            // 임의의 유효하지 않은 ObjectId 생성
            ObjectId invalidObjectId = ObjectId.fromString("0123456789012345678901234567890123456789");

            // when, then
            assertThrows(RuntimeException.class, () -> {
                gitService.getRevTree(repo, invalidObjectId);
            }, "유효하지 않은 ObjectId로부터 RevTree를 얻으려 할 때 RuntimeException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getTreeWalk")
    @DisplayName("유효한 Repository, RevTree, 파일경로가 주어졌을 때 TreeWalk를 반환해야 함")
    void getTreeWalk_WhenValidParameters_ShouldReturnTreeWalk() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        String testContent = "Test content";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), testContent);

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // 필요한 객체들 준비
            Repository repo = gitService.getRepository(git);
            ObjectId objectId = repo.resolve("HEAD");
            RevTree revTree = gitService.getRevTree(repo, objectId);

            // when
            TreeWalk treeWalk = gitService.getTreeWalk(repo, revTree, testFileName);

            // then
            assertNotNull(treeWalk, "TreeWalk 객체가 반환되어야 합니다");
            assertEquals(testFileName, treeWalk.getPathString(), "TreeWalk가 올바른 파일을 가리켜야 합니다");

            // TreeWalk 리소스 해제
            treeWalk.close();
        }
    }

    @Test
    @Tag("getTreeWalk")
    @DisplayName("존재하지 않는 파일 경로가 주어졌을 때 GitTreeWalkException이 발생해야 함")
    void getTreeWalk_WhenNonExistentFile_ShouldThrowGitTreeWalkException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        String testContent = "Test content";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), testContent);

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // 필요한 객체들 준비
            Repository repo = gitService.getRepository(git);
            ObjectId objectId = repo.resolve("HEAD");
            RevTree revTree = gitService.getRevTree(repo, objectId);

            // 존재하지 않는 파일 경로
            String nonExistentFileName = "non-existent.txt";

            // when, then
            assertThrows(GitTreeWalkException.class, () -> {
                gitService.getTreeWalk(repo, revTree, nonExistentFileName);
            }, "존재하지 않는 파일에 대해 GitTreeWalkException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getTreeWalk")
    @DisplayName("null Repository가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getTreeWalk_WhenNullRepo_ShouldThrowConstraintViolationException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), "Test content");

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // RevTree 얻기
            Repository repo = gitService.getRepository(git);
            ObjectId objectId = repo.resolve("HEAD");
            RevTree revTree = gitService.getRevTree(repo, objectId);

            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.getTreeWalk(null, revTree, testFileName);
            }, "null Repository로부터 TreeWalk를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getTreeWalk")
    @DisplayName("null RevTree가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getTreeWalk_WhenNullRevTree_ShouldThrowConstraintViolationException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            Repository repo = gitService.getRepository(git);

            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.getTreeWalk(repo, null, "test.txt");
            }, "null RevTree로부터 TreeWalk를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
        }
    }


    @Test
    @Tag("getCommitContents")
    @DisplayName("유효한 디렉토리, 커밋 해시, 파일 경로가 주어졌을 때 커밋의 파일 내용을 반환해야 함")
    void getCommitContents_WhenValidParameters_ShouldReturnFileContents() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 첫 번째 커밋
        String testFileName = "test.txt";
        String initialContent = "Initial content";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), initialContent);

        String commitHash = "";

        try (Git git = gitService.getGit(directory)) {
            // 첫 번째 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // 첫 번째 커밋 해시 저장
            RevCommit firstCommit = git.log().call().iterator().next();
            commitHash = firstCommit.getName();

            // 파일 내용 변경 및 두 번째 커밋
            String updatedContent = "Updated content";
            Files.writeString(testFile.toPath(), updatedContent);
            gitService.commit(git, testFileName, "Second commit", "Test Author", "test@example.com");

            // when
            String retrievedContent = gitService.getCommitContents(git, commitHash, testFileName);

            // then
            assertNotNull(retrievedContent, "파일 내용이 반환되어야 합니다");
            assertEquals(initialContent, retrievedContent, "반환된 내용이 첫 번째 커밋의 파일 내용과 일치해야 합니다");
        }
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("존재하지 않는 파일 경로가 주어졌을 때 GitTreeWalkException이 발생해야 함")
    void getCommitContents_WhenNonExistentFile_ShouldThrowGitTreeWalkException() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        String testContent = "Test content";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), testContent);

        String commitHash = "";

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // 커밋 해시 저장
            RevCommit commit = git.log().call().iterator().next();
            commitHash = commit.getName();

            // 존재하지 않는 파일 경로
            String nonExistentFileName = "non-existent.txt";

            // when, then
            String finalCommitHash = commitHash;
            assertThrows(GitTreeWalkException.class, () -> {
                gitService.getCommitContents(git, finalCommitHash, nonExistentFileName);
            }, "존재하지 않는 파일에 대해 GitTreeWalkException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("존재하지 않는 커밋 해시가 주어졌을 때 GitObjectIdNotFoundException이 발생해야 함")
    void getCommitContents_WhenNonExistentCommitHash_ShouldThrowGitObjectIdNotFoundException() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성
        String testFileName = "test.txt";
        String testContent = "Test content";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), testContent);

        try (Git git = gitService.getGit(directory)) {
            // 존재하지 않는 커밋 해시
            String nonExistentHash = "1234567890123456789012345678901234567890";

            // when, then
            assertThrows(GitRevTreeException.class, () -> {
                gitService.getCommitContents(git, nonExistentHash, testFileName);
            }, "존재하지 않는 커밋 해시에 대해 GitObjectIdNotFoundException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("git 저장소가 아닌 디렉토리가 주어졌을 때 GitFileOpenException이 발생해야 함")
    void getCommitContents_WhenDirectoryIsNotGitRepo_ShouldThrowGitFileOpenException() {
        // given
        File directory = tempDir.toFile();
        assertTrue(directory.exists(), "테스트 디렉토리가 존재해야 합니다");

        // 존재하지 않는 커밋 해시와 파일 경로 (테스트 목적으로만 사용)
        String someHash = "1234567890123456789012345678901234567890";
        String someFilePath = "some-file.txt";

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.getCommitContents(git, someHash, someFilePath);
        }, "Git 저장소가 아닌 디렉토리에서는 GitFileOpenException이 발생해야 합니다");
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("null 디렉토리가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getCommitContents_WhenNullDirectory_ShouldThrowConstraintViolationException() {
        // given
        String someHash = "1234567890123456789012345678901234567890";
        String someFilePath = "some-file.txt";

        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.getCommitContents(null, someHash, someFilePath);
        }, "null 디렉토리에 대해 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("null 커밋 해시가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getCommitContents_WhenNullCommitHash_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);
        String someFilePath = "some-file.txt";

        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.getCommitContents(git, null, someFilePath);
        }, "null 커밋 해시에 대해 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("getCommitContents")
    @DisplayName("null 파일 경로가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getCommitContents_WhenNullFilePath_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);
        String someHash = "1234567890123456789012345678901234567890";

        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.getCommitContents(git, someHash, null);
        }, "null 파일 경로에 대해 ConstraintViolationException이 발생해야 합니다");
    }


    @Test
    @Tag("revertLastCommit")
    @DisplayName("유효한 커밋이 있는 git 저장소에서 마지막 커밋을 성공적으로 되돌려야 함")
    void revertLastCommit_WhenValidRepo_ShouldRevertLastCommit() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 첫 번째 파일 생성 및 커밋
        String firstFileName = "first.txt";
        String firstContent = "First content";
        File firstFile = new File(directory, firstFileName);
        Files.writeString(firstFile.toPath(), firstContent);

        // 두 번째 파일 생성 (두 번째 커밋에서 추가될 파일)
        String secondFileName = "second.txt";
        String secondContent = "Second content";
        File secondFile = new File(directory, secondFileName);

        Git git = gitService.getGit(directory);

        // 첫 번째 커밋
        gitService.commit(git, firstFileName, "First commit", "Test Author", "test@example.com");

        // 첫 번째 커밋 후 상태 확인
        assertTrue(firstFile.exists(), "첫 번째 파일이 존재해야 합니다");
        assertEquals(1, StreamSupport.stream(git.log().call().spliterator(), false).count(), "커밋이 한 개 있어야 합니다");

        // 두 번째 파일 생성 및 커밋
        Files.writeString(secondFile.toPath(), secondContent);
        gitService.commit(git, secondFileName, "Second commit", "Test Author", "test@example.com");

        // 두 번째 커밋 후 상태 확인
        assertTrue(secondFile.exists(), "두 번째 파일이 존재해야 합니다");
        assertEquals(2, StreamSupport.stream(git.log().call().spliterator(), false).count(), "커밋이 두 개 있어야 합니다");

        // when
        gitService.revertLastCommit(git);

        // then
        // 되돌리기 후 커밋 개수 확인
        assertEquals(1, StreamSupport.stream(git.log().call().spliterator(), false).count(), "되돌리기 후 커밋이 한 개만 남아야 합니다");

        // 첫 번째 파일은 여전히 존재해야 함
        assertTrue(firstFile.exists(), "첫 번째 파일은 여전히 존재해야 합니다");
        assertEquals(firstContent, Files.readString(firstFile.toPath()), "첫 번째 파일 내용이 유지되어야 합니다");

        // 두 번째 파일은 작업 디렉토리에서 제거되어야 함 (clean 명령어 때문)
        assertFalse(secondFile.exists(), "두 번째 파일은 존재하지 않아야 합니다");
    }

    @Test
    @Tag("revertLastCommit")
    @DisplayName("커밋이 없는 git 저장소에서 GitHeadRefNotFoundException이 발생해야 함")
    void revertLastCommit_WhenEmptyRepo_ShouldThrowGitHeadRefNotFoundException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 초기화만 했고 커밋은 없는 상태

        // when, then
        assertThrows(GitHeadRefNotFoundException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.revertLastCommit(git);
        }, "커밋이 없는 저장소에서는 GitHeadRefNotFoundException이 발생해야 합니다");
    }

    @Test
    @Tag("revertLastCommit")
    @DisplayName("git 저장소가 아닌 디렉토리에서 GitFileOpenException이 발생해야 함")
    void revertLastCommit_WhenDirectoryIsNotGitRepo_ShouldThrowGitFileOpenException() {
        // given
        File directory = tempDir.toFile();
        assertTrue(directory.exists(), "테스트 디렉토리가 존재해야 합니다");

        // git 초기화를 하지 않은 일반 디렉토리

        // when, then
        assertThrows(GitFileOpenException.class, () -> {
            Git git = gitService.getGit(directory);
            gitService.revertLastCommit(git);
        }, "Git 저장소가 아닌 디렉토리에서는 GitFileOpenException이 발생해야 합니다");
    }

    @Test
    @Tag("revertLastCommit")
    @DisplayName("null 디렉토리에서 ConstraintViolationException이 발생해야 함")
    void revertLastCommit_WhenNullDirectory_ShouldThrowConstraintViolationException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.revertLastCommit(null);
        }, "null 디렉토리에 대해 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("revertLastCommit")
    @DisplayName("여러 커밋이 있는 git 저장소에서 마지막 커밋만 되돌려야 함")
    void revertLastCommit_WhenMultipleCommits_ShouldRevertOnlyLastCommit() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 첫 번째 파일 생성 및 커밋
        String firstFileName = "first.txt";
        String firstContent = "First content";
        File firstFile = new File(directory, firstFileName);
        Files.writeString(firstFile.toPath(), firstContent);

        // 두 번째 파일 생성
        String secondFileName = "second.txt";
        String secondContent = "Second content";
        File secondFile = new File(directory, secondFileName);

        // 세 번째 파일 생성
        String thirdFileName = "third.txt";
        String thirdContent = "Third content";
        File thirdFile = new File(directory, thirdFileName);

        Git git = gitService.getGit(directory);

        // 첫 번째 커밋
        gitService.commit(git, firstFileName, "First commit", "Test Author", "test@example.com");

        // 두 번째 파일 생성 및 커밋
        Files.writeString(secondFile.toPath(), secondContent);
        gitService.commit(git, secondFileName, "Second commit", "Test Author", "test@example.com");

        // 세 번째 파일 생성 및 커밋
        Files.writeString(thirdFile.toPath(), thirdContent);
        gitService.commit(git, thirdFileName, "Third commit", "Test Author", "test@example.com");

        // 세 번째 커밋 후 상태 확인
        assertEquals(3, StreamSupport.stream(git.log().call().spliterator(), false).count(), "커밋이 세 개 있어야 합니다");

        // when
        gitService.revertLastCommit(git);

        // then
        // 되돌리기 후 커밋 개수 확인
        assertEquals(2, StreamSupport.stream(git.log().call().spliterator(), false).count(), "되돌리기 후 커밋이 두 개만 남아야 합니다");

        // 첫 번째, 두 번째 파일은 여전히 존재해야 함
        assertTrue(firstFile.exists(), "첫 번째 파일은 여전히 존재해야 합니다");
        assertTrue(secondFile.exists(), "두 번째 파일은 여전히 존재해야 합니다");

        // 세 번째 파일은 작업 디렉토리에서 제거되어야 함
        assertFalse(thirdFile.exists(), "세 번째 파일은 존재하지 않아야 합니다");
    }

    @Test
    @Tag("getHeadRef")
    @DisplayName("커밋이 있는 git 저장소에서 HEAD 참조를 반환해야 함")
    void getHeadRef_WhenRepoHasCommits_ShouldReturnHeadRef() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        File testFile = new File(directory, testFileName);
        Files.writeString(testFile.toPath(), "Test content");

        try (Git git = gitService.getGit(directory)) {
            // 커밋 생성
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // when
            Ref headRef = gitService.getHeadRef(git);

            // then
            assertNotNull(headRef, "HEAD 참조가 반환되어야 합니다");
            assertEquals("HEAD", headRef.getName(), "참조 이름은 HEAD여야 합니다");
            assertNotNull(headRef.getObjectId(), "HEAD 참조는 유효한 ObjectId를 가져야 합니다");
            assertFalse(headRef.getObjectId().equals(ObjectId.zeroId()), "HEAD 참조는 zeroId가 아니어야 합니다");
        }
    }

    @Test
    @Tag("getHeadRef")
    @DisplayName("커밋이 없는 초기화된 git 저장소에서 GitHeadRefNotFoundException이 발생해야 함")
    void getHeadRef_WhenEmptyRepo_ShouldThrowGitHeadRefNotFoundException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // when, then
        try (Git git = gitService.getGit(directory)) {
            assertThrows(GitHeadRefNotFoundException.class, () -> {
                gitService.getHeadRef(git);
            }, "커밋이 없는 저장소에서는 GitHeadRefNotFoundException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("getHeadRef")
    @DisplayName("null Git 객체가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void getHeadRef_WhenNullGit_ShouldThrowNullPointerException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.getHeadRef(null);
        }, "null Git 객체로부터 HEAD 참조를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("getHeadRef")
    @DisplayName("여러 커밋이 있는 git 저장소에서 가장 최신 커밋을 가리키는 HEAD 참조를 반환해야 함")
    void getHeadRef_WhenMultipleCommits_ShouldReturnHeadRefPointingToLatestCommit() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성 및 커밋
        String testFileName = "test.txt";
        File testFile = new File(directory, testFileName);

        try (Git git = gitService.getGit(directory)) {
            // 첫 번째 커밋
            Files.writeString(testFile.toPath(), "Initial content");
            gitService.commit(git, testFileName, "Initial commit", "Test Author", "test@example.com");

            // 두 번째 커밋
            Files.writeString(testFile.toPath(), "Updated content");
            gitService.commit(git, testFileName, "Second commit", "Test Author", "test@example.com");

            // 최신 커밋의 ID 가져오기
            ObjectId latestCommitId = git.log().setMaxCount(1).call().iterator().next().getId();

            // when
            Ref headRef = gitService.getHeadRef(git);

            // then
            assertNotNull(headRef, "HEAD 참조가 반환되어야 합니다");

            // HEAD가 가리키는 실제 커밋 ID 가져오기 (HEAD는 refs/heads/master를 간접 참조할 수 있음)
            ObjectId headCommitId;
            if (headRef.isSymbolic()) {
                headCommitId = git.getRepository().resolve(headRef.getTarget().getName());
            } else {
                headCommitId = headRef.getObjectId();
            }

            assertEquals(latestCommitId, headCommitId, "HEAD는 가장 최신 커밋을 가리켜야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("커밋이 있는 git 저장소에서 전체 커밋 히스토리를 반환해야 함")
    void history_WhenRepoHasCommits_ShouldReturnAllCommits() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 3개의 커밋 생성
        try (Git git = gitService.getGit(directory)) {
            // 첫 번째 커밋
            File firstFile = new File(directory, "first.txt");
            Files.writeString(firstFile.toPath(), "First content");

            // git add를 명시적으로 실행
            git.add().addFilepattern("first.txt").call();

            // 첫 번째 커밋 생성 및 확인
            RevCommit firstCommit = git.commit()
                    .setMessage("First commit")
                    .setAuthor("Test Author", "test@example.com")
                    .call();
            assertNotNull(firstCommit, "첫 번째 커밋이 생성되어야 합니다");

            // 두 번째 커밋
            File secondFile = new File(directory, "second.txt");
            Files.writeString(secondFile.toPath(), "Second content");

            // git add를 명시적으로 실행
            git.add().addFilepattern("second.txt").call();

            // 두 번째 커밋 생성 및 확인
            RevCommit secondCommit = git.commit()
                    .setMessage("Second commit")
                    .setAuthor("Test Author", "test@example.com")
                    .call();
            assertNotNull(secondCommit, "두 번째 커밋이 생성되어야 합니다");

            // 세 번째 커밋
            File thirdFile = new File(directory, "third.txt");
            Files.writeString(thirdFile.toPath(), "Third content");

            // git add를 명시적으로 실행
            git.add().addFilepattern("third.txt").call();

            // 세 번째 커밋 생성 및 확인
            RevCommit thirdCommit = git.commit()
                    .setMessage("Third commit")
                    .setAuthor("Test Author", "test@example.com")
                    .call();
            assertNotNull(thirdCommit, "세 번째 커밋이 생성되어야 합니다");

            // 커밋 수 확인 (계속 진행하기 전에)
            long directCommitCount = StreamSupport.stream(git.log().call().spliterator(), false).count();
            assertEquals(3, directCommitCount, "커밋이 3개 생성되어야 계속 진행합니다");

            // when
            Iterable<RevCommit> history = gitService.history(git, null, null);

            // then
            assertNotNull(history, "커밋 히스토리가 반환되어야 합니다");

            // 커밋 수 확인
            List<RevCommit> commits = new ArrayList<>();
            history.forEach(commits::add);

            assertEquals(3, commits.size(), "3개의 커밋이 있어야 합니다");

            // 커밋 순서 확인 (최신 커밋이 먼저 나옴)
            assertEquals("Third commit", commits.get(0).getShortMessage(), "첫 번째로 반환된 커밋은 가장 최신 커밋이어야 합니다");
            assertEquals("Second commit", commits.get(1).getShortMessage(), "두 번째로 반환된 커밋은 두 번째 커밋이어야 합니다");
            assertEquals("First commit", commits.get(2).getShortMessage(), "세 번째로 반환된 커밋은 첫 번째 커밋이어야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("페이징 매개변수가 주어졌을 때 페이징된 커밋 히스토리를 반환해야 함")
    void history_WhenPagingParametersProvided_ShouldReturnPagedCommits() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 5개의 커밋 생성
        try (Git git = gitService.getGit(directory)) {
            for (int i = 1; i <= 5; i++) {
                String fileName = "file" + i + ".txt";
                File file = new File(directory, fileName);
                Files.writeString(file.toPath(), "Content " + i);
                gitService.commit(git, fileName, "Commit " + i, "Test Author", "test@example.com");
            }

            // when - 첫 번째 페이지 (최신 2개 커밋)
            Iterable<RevCommit> page1 = gitService.history(git, 1, 2);

            // then - 첫 번째 페이지 검증
            List<RevCommit> page1Commits = new ArrayList<>();
            page1.forEach(page1Commits::add);

            assertEquals(2, page1Commits.size(), "첫 번째 페이지는 2개의 커밋을 포함해야 합니다");
            assertEquals("Commit 5", page1Commits.get(0).getShortMessage(), "첫 번째 페이지의 첫 번째 커밋은 5번째 커밋이어야 합니다");
            assertEquals("Commit 4", page1Commits.get(1).getShortMessage(), "첫 번째 페이지의 두 번째 커밋은 4번째 커밋이어야 합니다");

            // when - 두 번째 페이지 (그 다음 2개 커밋)
            Iterable<RevCommit> page2 = gitService.history(git, 2, 2);

            // then - 두 번째 페이지 검증
            List<RevCommit> page2Commits = new ArrayList<>();
            page2.forEach(page2Commits::add);

            assertEquals(2, page2Commits.size(), "두 번째 페이지는 2개의 커밋을 포함해야 합니다");
            assertEquals("Commit 3", page2Commits.get(0).getShortMessage(), "두 번째 페이지의 첫 번째 커밋은 3번째 커밋이어야 합니다");
            assertEquals("Commit 2", page2Commits.get(1).getShortMessage(), "두 번째 페이지의 두 번째 커밋은 2번째 커밋이어야 합니다");

            // when - 세 번째 페이지 (마지막 1개 커밋)
            Iterable<RevCommit> page3 = gitService.history(git, 3, 2);

            // then - 세 번째 페이지 검증
            List<RevCommit> page3Commits = new ArrayList<>();
            page3.forEach(page3Commits::add);

            assertEquals(1, page3Commits.size(), "세 번째 페이지는 1개의 커밋을 포함해야 합니다");
            assertEquals("Commit 1", page3Commits.get(0).getShortMessage(), "세 번째 페이지의 첫 번째 커밋은 1번째 커밋이어야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("페이지 크기가 전체 커밋 수보다 큰 경우 모든 커밋을 반환해야 함")
    void history_WhenPageSizeLargerThanTotalCommits_ShouldReturnAllCommits() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 3개의 커밋 생성
        try (Git git = gitService.getGit(directory)) {
            for (int i = 1; i <= 3; i++) {
                String fileName = "file" + i + ".txt";
                File file = new File(directory, fileName);
                Files.writeString(file.toPath(), "Content " + i);
                gitService.commit(git, fileName, "Commit " + i, "Test Author", "test@example.com");
            }

            // when - 페이지 크기가 전체 커밋 수보다 큰 경우
            Iterable<RevCommit> history = gitService.history(git, 1, 10);

            // then
            List<RevCommit> commits = new ArrayList<>();
            history.forEach(commits::add);

            assertEquals(3, commits.size(), "모든 커밋이 반환되어야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("커밋이 없는 git 저장소에서 GitHistoryNotFoundException이 발생해야 함")
    void history_WhenEmptyRepo_ShouldThrowGitHistoryNotFoundException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // when, then
        try (Git git = gitService.getGit(directory)) {
            assertThrows(GitHistoryNotFoundException.class, () -> {
                gitService.history(git, null, null);
            }, "커밋이 없는 저장소에서는 GitHistoryNotFoundException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("null Git 객체가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void history_WhenNullGit_ShouldThrowConstraintViolationException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.history(null, 1, 10);
        }, "null Git 객체로부터 커밋 히스토리를 얻으려 할 때 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("history")
    @DisplayName("페이지가 0이고 크기가 유효한 경우 전체 커밋을 반환해야 함")
    void history_WhenPageIsZeroAndSizeValid_ShouldReturnAllCommits() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 3개의 커밋 생성
        try (Git git = gitService.getGit(directory)) {
            for (int i = 1; i <= 3; i++) {
                String fileName = "file" + i + ".txt";
                File file = new File(directory, fileName);
                Files.writeString(file.toPath(), "Content " + i);
                gitService.commit(git, fileName, "Commit " + i, "Test Author", "test@example.com");
            }

            // when
            Iterable<RevCommit> history = gitService.history(git, 0, 10);

            // then
            List<RevCommit> commits = new ArrayList<>();
            history.forEach(commits::add);

            assertEquals(3, commits.size(), "페이지가 0일 때 모든 커밋이 반환되어야 합니다");
        }
    }

    @Test
    @Tag("history")
    @DisplayName("크기가 0이고 페이지가 유효한 경우 전체 커밋을 반환해야 함")
    void history_WhenSizeIsZeroAndPageValid_ShouldReturnAllCommits() throws IOException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 3개의 커밋 생성
        try (Git git = gitService.getGit(directory)) {
            for (int i = 1; i <= 3; i++) {
                String fileName = "file" + i + ".txt";
                File file = new File(directory, fileName);
                Files.writeString(file.toPath(), "Content " + i);
                gitService.commit(git, fileName, "Commit " + i, "Test Author", "test@example.com");
            }

            // when
            Iterable<RevCommit> history = gitService.history(git, 1, 0);

            // then
            List<RevCommit> commits = new ArrayList<>();
            history.forEach(commits::add);

            assertEquals(3, commits.size(), "크기가 0일 때 모든 커밋이 반환되어야 합니다");
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("유효한 Git 객체와 파일이 주어졌을 때 성공적으로 커밋해야 함")
    void commit_WhenValidGitAndFile_ShouldCommitSuccessfully() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성
        String fileName = "test.txt";
        String fileContent = "Test content";
        File testFile = new File(directory, fileName);
        Files.writeString(testFile.toPath(), fileContent);

        // when
        try (Git git = gitService.getGit(directory)) {
            gitService.commit(git, fileName, "Test commit message", "Test Author", "test@example.com");

            // then
            // 커밋이 생성되었는지 확인
            Iterable<RevCommit> logs = git.log().call();
            RevCommit commit = logs.iterator().next();

            assertEquals("Test commit message", commit.getShortMessage(), "커밋 메시지가 일치해야 합니다");
            assertEquals("Test Author", commit.getAuthorIdent().getName(), "작성자 이름이 일치해야 합니다");
            assertEquals("test@example.com", commit.getAuthorIdent().getEmailAddress(), "작성자 이메일이 일치해야 합니다");

            // 파일이 커밋에 포함되었는지 확인
            TreeWalk treeWalk = gitService.getTreeWalk(git.getRepository(), commit.getTree(), fileName);
            assertNotNull(treeWalk, "커밋된 파일이 존재해야 합니다");
            treeWalk.close();
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("null Git 객체가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void commit_WhenNullGit_ShouldThrowConstraintViolationException() {
        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            gitService.commit(null, "test.txt", "Test commit", "Test Author", "test@example.com");
        }, "null Git 객체로 커밋 시도 시 ConstraintViolationException이 발생해야 합니다");
    }

    @Test
    @Tag("commit")
    @DisplayName("null 파일 패턴이 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void commit_WhenNullFilePattern_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        try (Git git = gitService.getGit(directory)) {
            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.commit(git, null, "Test commit", "Test Author", "test@example.com");
            }, "null 파일 패턴으로 커밋 시도 시 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("null 커밋 메시지가 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void commit_WhenNullCommitMessage_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성
        String fileName = "test.txt";
        File testFile = new File(directory, fileName);
        try {
            Files.writeString(testFile.toPath(), "Test content");
        } catch (IOException e) {
            fail("테스트 파일 생성 중 오류 발생: " + e.getMessage());
        }

        try (Git git = gitService.getGit(directory)) {
            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.commit(git, fileName, null, "Test Author", "test@example.com");
            }, "null 커밋 메시지로 커밋 시도 시 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("null 작성자 이름이 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void commit_WhenNullAuthorName_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성
        String fileName = "test.txt";
        File testFile = new File(directory, fileName);
        try {
            Files.writeString(testFile.toPath(), "Test content");
        } catch (IOException e) {
            fail("테스트 파일 생성 중 오류 발생: " + e.getMessage());
        }

        try (Git git = gitService.getGit(directory)) {
            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.commit(git, fileName, "Test commit", null, "test@example.com");
            }, "null 작성자 이름으로 커밋 시도 시 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("null 이메일이 주어졌을 때 ConstraintViolationException이 발생해야 함")
    void commit_WhenNullEmail_ShouldThrowConstraintViolationException() {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 파일 생성
        String fileName = "test.txt";
        File testFile = new File(directory, fileName);
        try {
            Files.writeString(testFile.toPath(), "Test content");
        } catch (IOException e) {
            fail("테스트 파일 생성 중 오류 발생: " + e.getMessage());
        }

        try (Git git = gitService.getGit(directory)) {
            // when, then
            assertThrows(ConstraintViolationException.class, () -> {
                gitService.commit(git, fileName, "Test commit", "Test Author", null);
            }, "null 이메일로 커밋 시도 시 ConstraintViolationException이 발생해야 합니다");
        }
    }

    @Test
    @Tag("commit")
    @DisplayName("여러 파일을 커밋할 때 모든 파일이 커밋되어야 함")
    void commit_WhenMultipleFiles_ShouldCommitAllFiles() throws IOException, GitAPIException {
        // given
        File directory = tempDir.toFile();
        gitService.init(directory);

        // 첫 번째 파일 생성
        String fileName1 = "file1.txt";
        File file1 = new File(directory, fileName1);
        Files.writeString(file1.toPath(), "Content 1");

        // 두 번째 파일 생성
        String fileName2 = "file2.txt";
        File file2 = new File(directory, fileName2);
        Files.writeString(file2.toPath(), "Content 2");

        try (Git git = gitService.getGit(directory)) {
            // 첫 번째 파일 커밋
            gitService.commit(git, fileName1, "Add file1", "Test Author", "test@example.com");

            // 두 번째 파일 커밋
            gitService.commit(git, fileName2, "Add file2", "Test Author", "test@example.com");

            // 두 개의 커밋이 생성되었는지 확인
            long commitCount = StreamSupport.stream(git.log().call().spliterator(), false).count();
            assertEquals(2, commitCount, "두 개의 커밋이 생성되어야 합니다");

            // 두 파일 모두 커밋에 포함되었는지 확인
            RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
            TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), fileName2, latestCommit.getTree());
            assertNotNull(treeWalk, "두 번째 파일이 커밋에 포함되어야 합니다");

            RevCommit previousCommit = StreamSupport.stream(git.log().setMaxCount(2).call().spliterator(), false)
                    .skip(1)
                    .findFirst()
                    .orElse(null);
            assertNotNull(previousCommit, "이전 커밋이 존재해야 합니다");

            TreeWalk previousTreeWalk = TreeWalk.forPath(git.getRepository(), fileName1, previousCommit.getTree());
            assertNotNull(previousTreeWalk, "첫 번째 파일이 이전 커밋에 포함되어야 합니다");
        }
    }

}
