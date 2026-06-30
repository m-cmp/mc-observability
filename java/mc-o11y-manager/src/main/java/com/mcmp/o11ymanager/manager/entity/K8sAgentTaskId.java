package com.mcmp.o11ymanager.manager.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Composite primary key for {@link K8sAgentTaskEntity}: (nsId, clusterId, nodeName). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class K8sAgentTaskId implements Serializable {

    private String nsId;
    private String clusterId;
    private String nodeName;
}
