package mcmp.mc.observability.agent.trigger.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TriggerAlertInfo {

    private Long policySeq;
    private String policyName;
    private Long targetSeq;
    private String targetId;
    private String nsId;
    private String targetName;
    private String metric;
    private String data;
    private String level;
    private String threshold;
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