package mcmp.mc.observability.agent.common.util;

import mcmp.mc.observability.agent.monitoring.model.MeasurementFieldInfo;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class InfluxDBUtils {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDBUtils.class);

    private InfluxDBUtils() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }
    public static List<MeasurementFieldInfo> measurementAndFieldsMapping(List<QueryResult.Series> seriesList) {
        List<MeasurementFieldInfo> result = new ArrayList<>();
        try {
            if(CollectionUtils.isEmpty(seriesList))
                return null;

            for( QueryResult.Series series : seriesList) {
                MeasurementFieldInfo measurementFieldInfo = new MeasurementFieldInfo();
                measurementFieldInfo.setMeasurement(series.getName());

                for (List<Object> rows : series.getValues()) {
                    MeasurementFieldInfo.FieldInfo fieldInfo = new MeasurementFieldInfo.FieldInfo();

                    for (int i = 0; i < series.getColumns().size(); i++) {
                        if (rows.get(i) == null) {
                            fieldInfo.getClass().getDeclaredMethod(getSetterMethodName(fieldInfo.getClass().getDeclaredField(series.getColumns().get(i)).getName()), fieldInfo.getClass().getDeclaredField(series.getColumns().get(i)).getType()).invoke(fieldInfo, rows.get(i));
                            continue;
                        }
                        logger.debug("Class=[{}, Method={}, Type={}], Column={}, Data=[Type={}, value={}]"
                                , fieldInfo.getClass(), getSetterMethodName(series.getColumns().get(i)), fieldInfo.getClass().getDeclaredField(series.getColumns().get(i))
                                , series.getColumns().get(i)
                                , rows.get(i).getClass(), rows.get(i));

                        fieldInfo.getClass().getDeclaredMethod(getSetterMethodName(series.getColumns().get(i)), rows.get(i).getClass()).invoke(fieldInfo, rows.get(i));
                    }
                    List<MeasurementFieldInfo.FieldInfo> fields = measurementFieldInfo.getFields();
                    fields.add(fieldInfo);
                    measurementFieldInfo.setFields(fields);
                }
                result.add(measurementFieldInfo);
            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                 NoSuchFieldException | NullPointerException e  ) {
            logger.error("e : {}", e.getMessage());
            return result;
        }

        return result;
    }

    private static String getSetterMethodName(String fieldName) {
        StringBuilder sb = new StringBuilder("set");
        sb.append(fieldName.substring(0, 1).toUpperCase());
        sb.append(fieldName.substring(1));
        return sb.toString();
    }
}
