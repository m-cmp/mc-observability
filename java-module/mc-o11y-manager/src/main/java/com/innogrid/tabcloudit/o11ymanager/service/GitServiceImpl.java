package com.innogrid.tabcloudit.o11ymanager.service;

import com.innogrid.tabcloudit.o11ymanager.exception.git.*;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.GitService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Validated
@Service
public class GitServiceImpl implements GitService {

  private final String HEAD_REF = "HEAD";

  public void init(@NotNull File directory) {
    try {
      Git.init().setDirectory(directory).call().close();
      log.info("[GitService] Git initialized at {}", directory.getAbsolutePath());
    } catch (GitAPIException e) {
      log.error("[GitService] Failed to init git", e);
      throw new GitInitException();
    }
  }

  public String getHashName(@NotNull Git git) {
    try {
      String name = git.log().call().iterator().next().getName();
      log.info("[GitService] Git log at {}", name);
      return name;
    } catch (GitAPIException e) {
      log.error("[GitService] Git hash value not found", e);
      throw new GitHashNotFoundException();
    }
  }

  public Git getGit(@NotNull File directory) {
    try {
      return Git.open(directory);
    } catch (IOException e) {
      log.error("[GitService] Failed to open git file", e);
      throw new GitFileOpenException();
    }
  }

  public Repository getRepository(@NotNull Git git) {
    return git.getRepository();
  }

  public ObjectId getObjectId(@NotNull Git git, @NotNull String hashName) {
    try {
      return getRepository(git).resolve(hashName);
    } catch (IOException e) {
      log.error("[GitService] Object ID not found", e);
      throw new GitObjectIdNotFoundException();
    }
  }

  public RevTree getRevTree(@NotNull Repository repo, @NotNull ObjectId objectId) {
    RevWalk revWalk = null;

    try {
      revWalk = new RevWalk(repo);
      revWalk.parseCommit(objectId);
      RevCommit commit = revWalk.parseCommit(objectId);
      return revWalk.parseTree(commit);
    } catch (IOException e) {
      throw new GitRevTreeException();
    } finally {
      Objects.requireNonNull(revWalk).dispose();
    }
  }

  public TreeWalk getTreeWalk(@NotNull Repository repo, @NotNull RevTree revTree, String filePath) {
    try (TreeWalk treeWalk = new TreeWalk(repo)) {
      log.info("[RUN}====================Start getTreeWalk====================");

      log.info("[GitService] Start getTreeWalk - filePath: {}", filePath);
      treeWalk.addTree(revTree);
      treeWalk.setRecursive(true);

      if (filePath != null) {
        treeWalk.setFilter(PathFilter.create(filePath));
      }

      if (!treeWalk.next()) {
        log.error("[GitService] Git tree walk next none");
        throw new GitTreeWalkException();
      }

      log.info("[GitService] Git tree walk at {}", filePath);
      log.info("[DONE}====================End getTreeWalk====================");
      return treeWalk;
    } catch (CorruptObjectException | IncorrectObjectTypeException | MissingObjectException e) {
      log.error("[GitService] Failed to open git tree walk", e);
      throw new GitTreeWalkException();
    } catch (IOException e) {
      log.error("[GitService] A loose object or pack file could not be read", e);
      throw new GitTreeWalkException();
    }
  }

  public String getCommitContents(@NotNull Git git, @NotNull String commitHash,
      @NotNull String filePath) {
    Repository repo = getRepository(git);
    log.info("[GitService] Getting repository for git: {}", repo.getDirectory());
    ObjectId objectId = getObjectId(git, commitHash);
    log.info("[GitService] Resolved ObjectId for commitHash [{}]: {}", commitHash, objectId.name());
    RevTree revTree = getRevTree(repo, objectId);
    TreeWalk treeWalk = getTreeWalk(repo, revTree, filePath);
    log.info("[GitService] Resolved RevTree: {}", revTree.getId());

    try {
      ObjectId targetObjectId = treeWalk.getObjectId(0);
      ObjectLoader loader = repo.open(targetObjectId);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      loader.copyTo(out);

      return out.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("[GitService] Failed to load content from Git object", e);
      throw new GitCommitContentsException();
    }
  }

  // revert
  public void revertLastCommit(@NotNull Git git) {
    Ref headRef = getHeadRef(git);

    if (headRef == null) {
      throw new GitHeadRefNotFoundException();
    }

    try {
      git.reset().setMode(ResetCommand.ResetType.MIXED).setRef("HEAD~1").call();
      git.checkout().setAllPaths(true).call();
      git.clean().setCleanDirectories(true).setForce(true).call();
    } catch (GitAPIException | JGitInternalException e) {
      throw new GitRevertException();
    }
  }

  public Ref getHeadRef(@NotNull Git git) {
    try {
      Ref ref = getRepository(git).findRef(HEAD_REF);

      // HEAD가 없는 경우
      if (ref.getObjectId() == null) {
        throw new GitHeadRefNotFoundException();
      }

      return ref;
    } catch (IOException e) {
      throw new GitHeadRefNotFoundException();
    }
  }

  public Iterable<RevCommit> history(@NotNull Git git, Integer page, Integer size) {
    try {
      LogCommand command = git.log();

      if (page != null && page != 0 && size != null && size != 0) {
        command.setSkip((page - 1) * size);
        command.setMaxCount(size);
      }

      return command.call();
    } catch (GitAPIException e) {
      throw new GitHistoryNotFoundException();
    }
  }

  public void commit(@NotNull Git git, @NotNull String filePattern,
      @NotNull String commitMessage, @NotNull String authorName, @NotNull String email) {
    try {
      git.add().addFilepattern(filePattern).call();
      git.commit()
          .setMessage(commitMessage)
          .setAuthor(authorName, email)
          .call();
    } catch (GitAPIException e) {
      throw new GitCommitFailureException();
    }
  }
}
