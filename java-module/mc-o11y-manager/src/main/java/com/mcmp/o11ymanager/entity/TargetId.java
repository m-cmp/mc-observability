package com.mcmp.o11ymanager.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TargetId implements Serializable {
  private String nsId;
  private String mciId;
  private String targetId;
}
