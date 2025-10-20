package com.mcmp.o11ymanager.manager.dto.vm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class VMDTO {

    @JsonProperty("vm_id")
    private String vmId;

    private String name;

    private String description;

    @JsonProperty("influx_seq")
    private Long influxSeq;

    @JsonProperty("vm_status")
    private VMStatus vmStatus;

    @JsonProperty("monitoring_agent_task_status")
    @JsonIgnore
    private VMAgentTaskStatus monitoringAgentTaskStatus;

    @JsonProperty("log_agent_task_status")
    @JsonIgnore
    private VMAgentTaskStatus logAgentTaskStatus;

    @JsonProperty("vm_monitoring_agent_task_id")
    @JsonIgnore
    private String vmMonitoringAgentTaskId;

    @JsonProperty("vm_log_agent_task_id")
    @JsonIgnore
    private String vmLogAgentTaskId;

    @JsonProperty("monitoring_service_status")
    private AgentServiceStatus monitoringServiceStatus;

    @JsonProperty("log_service_status")
    private AgentServiceStatus logServiceStatus;

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("mci_id")
    private String mciId;

    @JsonProperty("monitoring_agent_status")
    private AgentStatus monitoringAgentStatus;

    @JsonProperty("log_agent_status")
    private AgentStatus logAgentStatus;

    public static VMDTO fromEntity(VMEntity entity) {
        return VMDTO.builder()
                .vmId(entity.getVmId())
                .name(entity.getName())
                .description(entity.getDescription())
                .influxSeq(entity.getInfluxSeq())
                .vmStatus(entity.getVmStatus())
                .description(entity.getDescription())
                .vmMonitoringAgentTaskId(entity.getVmMonitoringAgentTaskId())
                .vmLogAgentTaskId(entity.getVmLogAgentTaskId())
                .monitoringServiceStatus(entity.getMonitoringServiceStatus())
                .logServiceStatus(entity.getLogServiceStatus())
                .monitoringAgentStatus(entity.getMonitoringAgentStatus())
                .logAgentStatus(entity.getLogAgentStatus())
                .nsId(entity.getNsId())
                .mciId(entity.getMciId())
                .build();
    }

    public VMEntity toEntity() {
        return VMEntity.builder()
                .vmId(this.getVmId())
                .name(this.name)
                .influxSeq(this.getInfluxSeq())
                .vmStatus(this.getVmStatus())
                .description(this.description)
                .monitoringAgentTaskStatus(this.monitoringAgentTaskStatus)
                .logAgentTaskStatus(this.logAgentTaskStatus)
                .vmMonitoringAgentTaskId(this.vmMonitoringAgentTaskId)
                .vmLogAgentTaskId(this.vmLogAgentTaskId)
                .monitoringServiceStatus(this.monitoringServiceStatus)
                .logServiceStatus(this.logServiceStatus)
                .monitoringAgentStatus(this.monitoringAgentStatus)
                .logAgentStatus(this.logAgentStatus)
                .nsId(this.nsId)
                .mciId(this.mciId)
                .build();
    }
}
