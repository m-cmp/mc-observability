package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageTriggerTargetStorageInfo {

    @JsonProperty("seq")
    private Long seq;

    @JsonProperty("target_seq")
    private Long targetSeq;

    @JsonProperty("policy_seq")
    private Long policySeq;

    @JsonProperty("url")
    private String url;

    @JsonProperty("database")
    private String database;

    @JsonProperty("retention_policy")
    private String retentionPolicy;

    @JsonProperty("created_at")
    private Timestamp createdAt;
}
