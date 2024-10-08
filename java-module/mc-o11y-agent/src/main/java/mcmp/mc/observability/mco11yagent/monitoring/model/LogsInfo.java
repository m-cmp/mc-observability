package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public class LogsInfo {

    @JsonProperty("opensearch_seq")
    private Long opensearchSeq;

    @JsonProperty("range")
    private String range;

    @JsonProperty("limit")
    private Long limit;

    @JsonProperty("conditions")
    private List<ConditionInfo> conditions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionInfo {

        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;
    }
}
