package com.mcmp.o11ymanager.manager.entity;

import com.mcmp.o11ymanager.manager.enums.Agent;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Composite primary key for {@link AgentInstallFailureEntity}: (nsId, infraId, nodeId, agent). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AgentInstallFailureId implements Serializable {

    private String nsId;
    private String infraId;
    private String nodeId;
    private Agent agent;
}
