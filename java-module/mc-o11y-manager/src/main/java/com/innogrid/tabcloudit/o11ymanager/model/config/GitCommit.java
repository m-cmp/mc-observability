package com.innogrid.tabcloudit.o11ymanager.model.config;

import java.sql.Timestamp;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GitCommit {
  private String commitHash;
  private String message;
  private Timestamp timestamp;
}
