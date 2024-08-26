package mcmp.mc.observability.agent.trigger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.trigger.enums.TaskStatus;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyUpdateDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerPolicyInfo {

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Base64 Encoded value", example = "Y3B1IHVzYWdlX2lkbGUgY2hlY2sgcG9saWN5")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value = "Host description", example = "ZGVzY3JpcHRpb24=")
    @Base64EncodeField
    private String description;
    @ApiModelProperty(value = "Trigger target metric", example = "cpu")
    private String metric;
    @ApiModelProperty(value = "Trigger target metric field", example = "usage_idle")
    private String field;
    @ApiModelProperty(value = "Trigger target metric statistics", example = "min")
    private String statistics;
    @ApiModelProperty(value = "Base64 Encoded value",  example = "eyJjcml0IjogInZhbHVlID4gMjAiLCAid2FybiI6ICJ2YWx1ZSA+IDUwIn0=")
    @Base64EncodeField
    private String threshold;
    @ApiModelProperty(value = "Agent Manager IP", example = "http://localhost:18080")
    private String agentManagerIp;
    @ApiModelProperty(value = "Trigger Policy enablement status")
    private TaskStatus status;
    @ApiModelProperty(value = "Fields to group the data", example = "[\"cpu\"]")
    private List<String> groupFields;
    @JsonIgnore
    private String tickScript;

    @ApiModelProperty(value = "The time when the trigger policy was registered", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the trigger policy was updated")
    private String updateAt;

    public void setCreateDto(TriggerPolicyCreateDto dto) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.metric = dto.getMetric();
        this.groupFields = dto.getGroupFields();
        this.threshold = dto.getThreshold();
        this.field = dto.getField();
        this.statistics = dto.getStatistics();
        this.status = dto.getStatus();
        this.agentManagerIp = dto.getAgentManagerIp();
    }

    public void setUpdateDto(TriggerPolicyUpdateDto dto) {
        if(dto.getName() != null)
            this.name = dto.getName();
        if (dto.getDescription() != null)
            this.description = dto.getDescription();
        if (dto.getMetric() != null)
            this.metric = dto.getMetric();
        if (dto.getGroupFields() != null)
            this.groupFields = dto.getGroupFields();
        if (dto.getThreshold() != null)
            this.threshold = dto.getThreshold();
        if (dto.getField() != null)
            this.field = dto.getField();
        if (dto.getStatistics() != null)
            this.statistics = dto.getStatistics();
        if (dto.getStatus() != null)
            this.status = dto.getStatus();
        if (dto.getAgentManagerIp() != null)
            this.agentManagerIp = dto.getAgentManagerIp();
    }

    public void makeTickScript(TriggerPolicyInfo triggerPolicy) {
        String tickScript =
                "var db = '@DATABASE'\n" +
                "var rp = '@RETENTION_POLICY'\n" +
                "var measurement = '@MEASUREMENT'\n" +
                "var groupBy = @GROUP_BY\n\n" +
                "var streamData = stream\n" +
                "    |from()\n" +
                "        .database(db)\n" +
                "        .retentionPolicy(rp)\n" +
                "        .measurement(measurement)\n" +
                "        .groupBy(groupBy)\n" +
                "        .where(lambda: isPresent(\"@FIELD\")@WHERE_CONDITION)\n" +
                "    |eval()\n" +
                "        .keep('@FIELD')\n\n" +
                "var data = streamData\n" +
                "    |@STATISTICS('@FIELD')\n" +
                "        .as('value')\n\n" +
                "var trigger = data\n" +
                "    |alert()\n" +
                "@ALERT_CONDITION" +
                "        .id('{{ .TaskName }}')\n" +
                "        .stateChangesOnly()\n" +
                "        .post('@AGENT_MANAGER_IP/api/o11y/trigger/receiver')\n\n" +
                "trigger\n" +
                "    |httpOut('output')";

        String measurement = triggerPolicy.getMetric();
        String field = triggerPolicy.getField();
        String whereCondition = "";
        if("cpu".equals(measurement))
            whereCondition = " AND \"cpu\" != 'cpu-total'";
        String statistics = triggerPolicy.getStatistics();
        String alertCondition = getAlertCondition(triggerPolicy);

        tickScript = tickScript.replaceAll("@MEASUREMENT", measurement)
                .replaceAll("@FIELD", field)
                .replaceAll("@GROUP_BY", convertListToString(triggerPolicy.getGroupFields()))
                .replaceAll("@WHERE_CONDITION", whereCondition)
                .replaceAll("@STATISTICS", statistics)
                .replaceAll("@ALERT_CONDITION", alertCondition)
                .replaceAll("@AGENT_MANAGER_IP", triggerPolicy.getAgentManagerIp());

        this.tickScript = tickScript;
    }

    public void setTickScriptStorageInfo(String database, String retentionPolicy) {
        String script = this.tickScript;
        this.tickScript = script
                .replaceAll("@DATABASE", database)
                .replaceAll("@RETENTION_POLICY", retentionPolicy);
    }

    public String convertListToString(List<String> groupByFields) {
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("nsId");

        if (groupByFields != null && !groupByFields.isEmpty()) {
            fields.addAll(groupByFields);
        }
        String result = "['" + String.join("', '", fields) + "']";

        return result;
    }

    private String getAlertCondition(TriggerPolicyInfo triggerPolicy) {
        Map<String, String> thresholds = parseThresholds(triggerPolicy.getThreshold());
        StringBuilder alertCondition = new StringBuilder();
        String[] levels = {"crit", "warn", "info"};

        for (String level : levels) {
            if (thresholds.containsKey(level)) {
                alertCondition.append("        .")
                        .append(level)
                        .append("(lambda: ")
                        .append(thresholds.get(level).replace("value", "\"value\""))
                        .append(")\n");
            }
        }

        return String.valueOf(alertCondition);
    }

    private Map<String, String> parseThresholds(String thresholdJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(thresholdJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse threshold JSON", e);
        }
    }
}