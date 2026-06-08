package com.mcmp.o11ymanager.manager.entity;

import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class NodeId implements Serializable {
    private String nsId;
    private String infraId;
    private String nodeId;
}
