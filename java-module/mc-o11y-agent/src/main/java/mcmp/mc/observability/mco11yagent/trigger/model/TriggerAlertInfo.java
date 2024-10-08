package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TriggerAlertInfo {

    @JsonProperty("policy_seq")
    private Long policySeq;

    @JsonProperty("policy_name")
    private String policyName;

    @JsonProperty("target_seq")
    private Long targetSeq;

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("target_name")
    private String targetName;

    @JsonProperty("metric")
    private String metric;

    @JsonProperty("data")
    private String data;

    @JsonProperty("level")
    private String level;

    @JsonProperty("threshold")
    private String threshold;

    @JsonProperty("occur_time")
    private String occurTime;

    public String getAlertMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("*[Alert: Trigger event occurred]*\n\n");

        appendIfNotNull(sb, "Policy Name", this.policyName);
        appendIfNotNull(sb, "ID", this.targetId);
        appendIfNotNull(sb, "Namespace ID", this.nsId);
        appendIfNotNull(sb, "Target Name", this.targetName);
        appendIfNotNull(sb, "Metric", this.metric);
        appendIfNotNull(sb, "Level", this.level);
        appendIfNotNull(sb, "Threshold", this.threshold);
        appendIfNotNull(sb, "Occur Time", this.occurTime);

        return sb.toString();
    }

    private void appendIfNotNull(StringBuilder sb, String fieldName, String fieldValue) {
        if (fieldValue != null) {
            sb.append(fieldName).append(" : ").append(fieldValue).append("\n");
        }
    }
}
