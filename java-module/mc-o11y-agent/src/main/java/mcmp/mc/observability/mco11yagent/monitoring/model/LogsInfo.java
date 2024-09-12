
package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public class LogsInfo {
    private Long opensearchSeq;
    private String range;
    private Long limit;
    private List<ConditionInfo> conditions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionInfo {
        private String key;
        private String value;
    }
}
