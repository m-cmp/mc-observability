package com.mcmp.o11ymanager.manager.entity;

import java.io.Serializable;
import lombok.*;

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
