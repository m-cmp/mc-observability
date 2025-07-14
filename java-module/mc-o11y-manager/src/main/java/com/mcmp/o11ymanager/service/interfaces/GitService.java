package com.mcmp.o11ymanager.service.interfaces;

import jakarta.validation.constraints.NotNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;

public interface GitService {

    void init(@NotNull File directory);

    String getHashName(@NotNull Git git);

    Git getGit(@NotNull File directory);

    Repository getRepository(@NotNull Git git);

    ObjectId getObjectId(@NotNull Git git, @NotNull String hashName);

    RevTree getRevTree(@NotNull Repository repo, @NotNull ObjectId objectId);

    TreeWalk getTreeWalk(@NotNull Repository repo, @NotNull RevTree revTree, String filePath);

    String getCommitContents(@NotNull Git git, @NotNull String commitHash, @NotNull String filePath);

    void revertLastCommit(@NotNull Git git);

    Ref getHeadRef(@NotNull Git git);

    Iterable<RevCommit> history(@NotNull Git git, Integer page, Integer size);

    void commit(@NotNull Git git, @NotNull String filePattern,
                @NotNull String commitMessage, @NotNull String authorName, @NotNull String email);
}
