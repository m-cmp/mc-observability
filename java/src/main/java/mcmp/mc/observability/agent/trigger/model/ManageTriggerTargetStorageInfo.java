package mcmp.mc.observability.agent.trigger.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    private Long seq;
    private Long targetSeq;
    private Long policySeq;
    private String url;
    private String database;
    private String retentionPolicy;
    private Timestamp createdAt;
}
