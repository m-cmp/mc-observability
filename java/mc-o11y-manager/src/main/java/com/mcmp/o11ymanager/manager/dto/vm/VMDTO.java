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
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "VM ID", example = "vm-1")
    @JsonProperty("vm_id")
    private String vmId;

    @Schema(description = "VM name", example = "mcmp-vm")
    private String name;

    @Schema(description = "VM description", example = "string")
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

    @JsonProperty("trace_agent_task_status")
    @JsonIgnore
    private VMAgentTaskStatus traceAgentTaskStatus;

    @JsonProperty("vm_monitoring_agent_task_id")
    @JsonIgnore
    private String vmMonitoringAgentTaskId;

    @JsonProperty("vm_log_agent_task_id")
    @JsonIgnore
    private String vmLogAgentTaskId;

    @JsonProperty("vm_trace_agent_task_id")
    @JsonIgnore
    private String vmTraceAgentTaskId;

    @JsonProperty("monitoring_service_status")
    private AgentServiceStatus monitoringServiceStatus;

    @JsonProperty("log_service_status")
    private AgentServiceStatus logServiceStatus;

    @JsonProperty("trace_service_status")
    private AgentServiceStatus traceServiceStatus;

    @Schema(description = "Namespace ID", example = "ns-1")
    @JsonProperty("ns_id")
    private String nsId;

    @Schema(description = "MCI ID", example = "mci-1")
    @JsonProperty("mci_id")
    private String mciId;

    @Schema(description = "Monitoring agent status", example = "SUCCESS")
    @JsonProperty("monitoring_agent_status")
    private AgentStatus monitoringAgentStatus;

    @Schema(description = "Log agent status", example = "SUCCESS")
    @JsonProperty("log_agent_status")
    private AgentStatus logAgentStatus;

    @Schema(description = "Trace agent status", example = "SUCCESS")
    @JsonProperty("trace_agent_status")
    private AgentStatus traceAgentStatus;

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
                .vmTraceAgentTaskId(entity.getVmTraceAgentTaskId())
                .monitoringServiceStatus(entity.getMonitoringServiceStatus())
                .logServiceStatus(entity.getLogServiceStatus())
                .traceServiceStatus(entity.getTraceServiceStatus())
                .monitoringAgentStatus(entity.getMonitoringAgentStatus())
                .logAgentStatus(entity.getLogAgentStatus())
                .traceAgentStatus(entity.getTraceAgentStatus())
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
                .traceAgentTaskStatus(this.traceAgentTaskStatus)
                .vmMonitoringAgentTaskId(this.vmMonitoringAgentTaskId)
                .vmLogAgentTaskId(this.vmLogAgentTaskId)
                .vmTraceAgentTaskId(this.vmTraceAgentTaskId)
                .monitoringServiceStatus(this.monitoringServiceStatus)
                .logServiceStatus(this.logServiceStatus)
                .traceServiceStatus(this.traceServiceStatus)
                .monitoringAgentStatus(this.monitoringAgentStatus)
                .logAgentStatus(this.logAgentStatus)
                .traceAgentStatus(this.traceAgentStatus)
                .nsId(this.nsId)
                .mciId(this.mciId)
                .build();
    }
}
