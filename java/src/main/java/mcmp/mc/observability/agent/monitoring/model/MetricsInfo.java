package mcmp.mc.observability.agent.monitoring.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Setter
public class MetricsInfo {
    @ApiModelProperty(required = true, example = "1")
    private Long influxDBSeq;
    @ApiModelProperty(required = true, example = "cpu")
    private String measurement;
    @ApiModelProperty(required = true, example = "1h", notes = "timeunit s,m,h,d(second, minute, hour, day)")
    private String range;
    @ApiModelProperty(example = "1h", notes = "timeunit s,m,h,d(second, minute, hour, day)")
    private String groupTime;
    @ApiModelProperty(example = "[\"uuid\",\"cpu\"]")
    private List<String> groupBy;
    @ApiModelProperty(example = "10")
    private Long limit;
    @ApiModelProperty(example = "[{\"function\":\"mean\",\"field\":\"usage_idle\"}]")
    private List<FieldInfo> fields;
    @ApiModelProperty(example = "[{\"key\":\"cpu\",\"value\":\"cpu-total\"}]")
    private List<ConditionInfo> conditions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldInfo {
        private String function;
        private String field;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionInfo {
        private String key;
        private String value;
    }

    private String getFieldQuery() {
        if( fields == null || fields.isEmpty() ) return ", *";
        StringBuilder sb = new StringBuilder();
        for( FieldInfo field : fields ) {
            sb.append(", ");
            if( field.getFunction() == null ) sb.append(field.getField());
            else {
                sb.append(field.getFunction())
                    .append("(")
                    .append(field.getField())
                    .append(")")
                    .append(" as ")
                    .append(field.getField());
            }
        }
        return sb.toString();
    }

    private String getConditionQuery() {
        if( conditions == null || conditions.isEmpty() ) return "";
        StringBuilder sb = new StringBuilder();
        for( ConditionInfo conditionInfo : conditions ) {
            sb.append(" and ")
                    .append(conditionInfo.getKey())
                    .append("=")
                    .append("'")
                    .append(conditionInfo.getValue())
                    .append("'");
        }
        return sb.toString();
    }

    private String getGroupByQuery() {
        if( groupTime == null && (groupBy == null || groupBy.isEmpty()) ) return "";
        StringBuilder sb = new StringBuilder("group by ");

        if( groupTime != null ) sb.append(" time(").append(groupTime).append(")");
        if( groupBy != null && !groupBy.isEmpty() ) {
            if( !sb.toString().equals("group by ") ) sb.append(",");
            sb.append(String.join(",", groupBy));
        }

        return sb.toString();
    }

    private String getLimitQuery() {
        return (getLimit() != null && getLimit() > 0? "limit " + getLimit(): "");
    }

    @ApiModelProperty(hidden = true)
    public boolean isVaild() {
        if( fields !=  null && !fields.isEmpty() ) {
            boolean isSimple = false;
            boolean isFunction = false;

            for( FieldInfo info : fields) {
                if( info.getFunction() != null ) isFunction = true;
                else isSimple = true;
                if( info.getField() == null ) return false;
            }
            if( isSimple && isFunction ) return false;
        }

        if( conditions != null && !conditions.isEmpty() ) {
            for( ConditionInfo info : conditions ) {
                if( info.getKey().isEmpty() || info.getValue().isEmpty() ) return false;
            }
        }
        return true;
    }

    private void jsonParser(JsonObject js, Class<?> clazz, Object o) {
        List<Field> fieldList = Arrays.asList(clazz.getDeclaredFields());
        for( String key : js.keySet() ) {
            Optional<Field> optionalField = fieldList.stream().filter(f -> f.getName().equals(key)).findAny();
            if( !optionalField.isPresent() ) continue;
            Field field = optionalField.get();

            String targetMethodName = toUppercase(field.getName());

            if( field.getType().equals(Integer.class) ||
                field.getType().equals(Long.class) ||
                field.getType().equals(Boolean.class) ||
                field.getType().equals(String.class) ||
                field.getType().equals(Byte.class) ||
                field.getType().equals(Double.class) ||
                field.getType().equals(Float.class)
            ) {
                try {
                    Object value = null;

                    if( field.getType().equals(Integer.class) )    value = js.get(key).getAsInt();
                    if( field.getType().equals(Long.class) )       value = js.get(key).getAsLong();
                    if( field.getType().equals(Boolean.class) )    value = js.get(key).getAsBoolean();
                    if( field.getType().equals(String.class) )     value = js.get(key).getAsString();
                    if( field.getType().equals(Byte.class) )       value = js.get(key).getAsByte();
                    if( field.getType().equals(Double.class) )     value = js.get(key).getAsDouble();
                    if( field.getType().equals(Float.class) )      value = js.get(key).getAsFloat();
                    Arrays.stream(clazz.getDeclaredMethods()).filter(f -> f.getName().equals("set" + targetMethodName)).findAny().get().invoke(o, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
            else if( field.getType().equals(List.class) ) {
                Type[] types = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                if(types.length == 0) continue;

                try {
                    JsonArray array = js.get(key).getAsJsonArray();
                    if( ((Class<?>) types[0]).getName().contains("java") ) {
                        List<Object> oList = new ArrayList<>();
                        for( JsonElement je : array.asList() ) {
                            if( types[0].equals(Integer.class) )    oList.add(je.getAsInt());
                            if( types[0].equals(Long.class) )       oList.add(je.getAsLong());
                            if( types[0].equals(Boolean.class) )    oList.add(je.getAsBoolean());
                            if( types[0].equals(String.class) )     oList.add(je.getAsString());
                            if( types[0].equals(Byte.class) )       oList.add(je.getAsByte());
                            if( types[0].equals(Double.class) )     oList.add(je.getAsDouble());
                            if( types[0].equals(Float.class) )      oList.add(je.getAsFloat());
                        }
                        Arrays.stream(clazz.getDeclaredMethods()).filter(f -> f.getName().equals("set" + targetMethodName)).findAny().get().invoke(this, oList);
                    }
                    else {
                        List<Object> oList = new ArrayList<>();
                        for( JsonElement je : array.asList() ) {
                            Object o2 = ((Class) types[0]).getConstructor().newInstance();
                            jsonParser(je.getAsJsonObject(), ((Class) types[0]), o2);
                            oList.add(o2);
                        }
                        Arrays.stream(clazz.getDeclaredMethods()).filter(f -> f.getName().equals("set" + targetMethodName)).findAny().get().invoke(this, oList);
                    }
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    private static String toUppercase(String targetStr) {
        return targetStr.length() > 1 ? targetStr.substring(0, 1).toUpperCase() + targetStr.substring(1, targetStr.length()) : targetStr.substring(0, 1);
    }

    @ApiModelProperty(hidden = true)
    public String getQuery() {
        String query = "select time as timestamp @FIELD from @MEASUREMENT where time > now() - @RANGE @CONDITION @GROUP_BY order by time desc @LIMIT";
        query = query.replaceAll("@FIELD", getFieldQuery())
                     .replaceAll("@MEASUREMENT", getMeasurement())
                     .replaceAll("@RANGE", getRange())
                     .replaceAll("@CONDITION", getConditionQuery())
                     .replaceAll("@GROUP_BY", getGroupByQuery())
                     .replaceAll("@LIMIT", getLimitQuery());
        return query;
    }
}
