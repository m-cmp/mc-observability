package mcmp.mc.observability.agent.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.util.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class PageableReqBody<T> {
    private long page;
    private long rows;
    private String sidx;
    private String sord;
    private String q0;
    private String q1;
    private String q2;
    private T data;

    public long getPageNum() {
        if( page <= 0 || rows <= 0 ) return 0;
        return (page-1)* rows;
    }

    public String getOrder() {
        if( sidx == null || sidx.isEmpty() || sord == null || sord.isEmpty() ) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(" ORDER BY `");
        sb.append(Utils.camelToSnake(getSidx()));
        sb.append("` ");
        sb.append(sord.equalsIgnoreCase("asc")? "ASC": "DESC");
        return sb.toString();
    }

    public List<PageFilter> getFilter() {
        List<PageFilter> list = new ArrayList<>();
        if( q0 == null || q0.isEmpty() ) return list;

        String column[] = q0.split(",");
        String word[] = q1.split(",");
        String condition[] = q2.split(",");
        boolean isAll = false;
        if( condition[0].equalsIgnoreCase("all") ) isAll = true;

        List<String> fields = new ArrayList<>();

        for( Field f : data.getClass().getDeclaredFields() ) {
            fields.add(f.getName());
        }

        for( int i = 0 ; i < column.length ; i++ ) {
            if( !fields.contains(column[i]) ) continue;

            list.add(new PageFilter(column[i]
                    , (isAll?word[0]:word[i])
                    , (isAll?condition[0]:condition[i])
                    , (i!=0?(isAll?"OR":"AND"):""))
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