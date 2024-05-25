package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.model.*;
import mcmp.mc.observability.agent.util.InfluxDBUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBService {
//    private static final Map<Integer, InfluxDB> influxDBMap = new HashMap<>();
    private final HostStorageService hostStorageService;

    public List<InfluxDBConnector> getList() {
            List<HostStorageInfo> storageInfoList = hostStorageService.getList(new HashMap<>());
        if (CollectionUtils.isEmpty(storageInfoList))
            return null;

        List<InfluxDBConnector> influxDBConnectorList = new ArrayList<>();
        for (HostStorageInfo hostStorageInfo : storageInfoList) {
            InfluxDBConnector influxDBConnector = new InfluxDBConnector(hostStorageInfo.getSetting());
            influxDBConnectorList.add(influxDBConnector);
        }

        List<InfluxDBConnector> uniqueList = new ArrayList<>();

        try {
            uniqueList = new ArrayList<>(influxDBConnectorList.stream()
                    .collect(Collectors.toMap(
                            connector -> Arrays.asList(connector.getUrl(), connector.getDatabase(), connector.getUsername(), connector.getPassword()),
                            connector -> connector, (existing, replacement) -> existing))
                    .values());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return uniqueList;
    }

    public List<MeasurementFieldInfo> getMeasurementAndFields(InfluxDBConnector influxDBConnector) {
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery("show field keys").forDatabase(influxDBConnector.getDatabase());

        QueryResult qr = influxDBConnector.getInfluxDB().query(qb.create());

        return InfluxDBUtils.measurementAndFieldsMapping(qr.getResults().get(0).getSeries());
    }

    public List<Map<String, Object>> getTags(InfluxDBConnector influxDBConnector) {
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery("show tag keys").forDatabase(influxDBConnector.getDatabase());

        QueryResult qr = influxDBConnector.getInfluxDB().query(qb.create());

        List<Map<String, Object>> result = new ArrayList<>();
        List<QueryResult.Series> seriesList = qr.getResults().get(0).getSeries();

        if(CollectionUtils.isEmpty(seriesList))
            return null;

        for( QueryResult.Series series : seriesList) {
            Map<String, Object> measurementTags = new HashMap<>();
            List<String> tagList = new ArrayList<>();
            for( List<Object> row : series.getValues() ) {
                tagList.add(row.get(0).toString());
            }
            measurementTags.put(series.getName(), tagList);

            result.add(measurementTags);
        }

        return result;
    }

    public List<MetricInfo> getMetrics(MetricParamInfo metricParamInfo) {
        InfluxDBConnector influxDBConnector = new InfluxDBConnector(metricParamInfo);

        String queryString = String.format("select time as timestamp, %s from %s where time > now() - %s and uuid=$uuid group by time(%s), * fill(null) order by time desc limit %s"
                , metricParamInfo.getField(), metricParamInfo.getMeasurement(), metricParamInfo.getRange(), metricParamInfo.getGroupTime(), metricParamInfo.getLimit());

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder
                .newQuery(queryString)
                .forDatabase(influxDBConnector.getDatabase());

        qb.bind("uuid", metricParamInfo.getUuid());

        return getMetricInfos(qb.create(), influxDBConnector);
    }

    public List<MetricInfo> getMetricDatas(MetricDataParamInfo metricDataParamInfo) {
        InfluxDBConnector influxDBConnector = new InfluxDBConnector(metricDataParamInfo);
        String queryString = metricDataParamInfo.makeQueryString();

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder
                .newQuery(queryString)
                .forDatabase(influxDBConnector.getDatabase());

        return getMetricInfos(qb.create(), influxDBConnector);
    }

    private List<MetricInfo> getMetricInfos(Query query, InfluxDBConnector influxDBConnector) {

        QueryResult queryResult = influxDBConnector.getInfluxDB().query(query);

        List<QueryResult.Result> influxResults = queryResult.getResults();
        List<MetricInfo> result = new ArrayList<>();

        if( influxResults.isEmpty() || influxResults.get(0).getSeries() == null ) {
            return result;
        }

        influxResults.forEach(f -> f.getSeries().forEach(f2 -> result.add(new MetricInfo(f2.getName(), f2.getColumns(), f2.getTags(), f2.getValues()))));

        return result;
    }
}
