package mcmp.mc.observability.agent.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.annotation.Base64EncodeField;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

import static springfox.documentation.schema.property.bean.Accessors.toCamelCase;

@Getter
@Setter
public class MetricDataParamInfo {

    @NotBlank
    private String url;
    @NotBlank
    private String database;
    @NotBlank
    private String retentionPolicy;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String measurement;
    @NotBlank
    private String range; // 1s, 1m, 1h...
    @NotBlank
    private String groupTime;
    private List<FieldFunctionInfo> fieldFunctionInfoList;
    private List<ConditionInfo> conditionInfoList;
    @NotNull
    private Long limit;

    @Getter
    @Setter
    public static class FieldFunctionInfo {
        private String field;
        private String function;
    }

    @Getter
    @Setter
    public static class ConditionInfo {
        private String condition;
        private String value;
    }

    public String makeQueryString() {
        StringBuilder sb = new StringBuilder("select time as timestamp, ");
        boolean isFunction = false;
        if(CollectionUtils.isEmpty(fieldFunctionInfoList)) {
            sb.append("* ");
        } else {
            for(FieldFunctionInfo fieldFunction : fieldFunctionInfoList) {
                if(fieldFunction.getFunction() == null) {
                    sb.append(fieldFunction.getField());
                } else {
                    sb.append(fieldFunction.getFunction())
                            .append("(")
                            .append(fieldFunction.getField())
                            .append(")")
                            .append(" as ")
                            .append(fieldFunction.getField())
                            .append(" ");
                    isFunction = true;
                }
            }
        }
        sb.append("from ")
                .append(measurement)
                .append(" where time > now() - ")
                .append(range);


        if(!CollectionUtils.isEmpty(conditionInfoList)) {
            for (ConditionInfo conditionInfo : conditionInfoList) {
                sb.append(" and ")
                        .append(conditionInfo.getCondition())
                        .append("=")
                        .append("'")
                        .append(conditionInfo.getValue())
                        .append("'");
            }
        }

        if(isFunction) {
            sb.append(" group by time(")
                    .append(groupTime)
                    .append("), *");
        }

        sb.append(" fill(null)")
            .append(" order by time desc ")
            .append("limit ")
            .append(limit);


        return sb.toString();
    }
}
