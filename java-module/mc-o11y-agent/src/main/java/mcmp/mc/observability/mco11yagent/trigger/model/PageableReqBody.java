package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.trigger.util.TriggerUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class PageableReqBody<T> {
    @JsonProperty("page")
    private long page;

    @JsonProperty("rows")
    private long rows;

    @JsonProperty("sidx")
    private String sidx;

    @JsonProperty("sord")
    private String sord;

    @JsonProperty("q0")
    private String q0;

    @JsonProperty("q1")
    private String q1;

    @JsonProperty("q2")
    private String q2;

    @JsonProperty("data")
    private T data;

    public long getPageNum() {
        if (page <= 0 || rows <= 0) return 0;
        return (page - 1) * rows;
    }

    public String getOrder() {
        if (sidx == null || sidx.isEmpty() || sord == null || sord.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(" ORDER BY `");
        sb.append(TriggerUtils.camelToSnake(getSidx()));
        sb.append("` ");
        sb.append(sord.equalsIgnoreCase("asc") ? "ASC" : "DESC");
        return sb.toString();
    }

    public List<PageFilter> getFilter() {
        List<PageFilter> list = new ArrayList<>();
        if (q0 == null || q0.isEmpty()) return list;

        String column[] = q0.split(",");
        String word[] = q1.split(",");
        String condition[] = q2.split(",");
        boolean isAll = false;
        if (condition[0].equalsIgnoreCase("all")) isAll = true;

        List<String> fields = new ArrayList<>();

        for (Field f : data.getClass().getDeclaredFields()) {
            fields.add(f.getName());
        }

        for (int i = 0; i < column.length; i++) {
            if (!fields.contains(column[i])) continue;

            list.add(new PageFilter(column[i],
                    (isAll ? word[0] : word[i]),
                    (isAll ? condition[0] : condition[i]),
                    (i != 0 ? (isAll ? "OR" : "AND") : ""))
            );
        }

        return list;
    }

    @Getter
    @AllArgsConstructor
    public static class PageFilter {
        private String column;
        private String word;
        private String condition;
        private String separator;
    }
}
