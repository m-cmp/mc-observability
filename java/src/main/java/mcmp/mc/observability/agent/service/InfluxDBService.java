package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.model.MetricParamInfo;
import mcmp.mc.observability.agent.util.InfluxDBUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Pong;
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
    private static final Map<Integer, InfluxDB> influxDBMap = new HashMap<>();
    private final HostStorageService hostStorageService;

    public InfluxDB getDB(String setting) {
        InfluxDBConnector vo = new InfluxDBConnector(setting);
        return getDB(vo);
    }

    public InfluxDB getDB(InfluxDBConnector influxDBConnector) {
        if( influxDBMap.get(influxDBConnector.hashCode()) == null ) {
            InfluxDB influxDB = ((influxDBConnector.getUsername() == null || influxDBConnector.getUsername().isEmpty())?
                    InfluxDBFactory.connect(influxDBConnector.getUrl()):
                    InfluxDBFactory.connect(influxDBConnector.getUrl(), influxDBConnector.getUsername(), influxDBConnector.getPassword()))
                    .setDatabase(influxDBConnector.getDatabase())
                    .setRetentionPolicy(influxDBConnector.getRetentionPolicy());
            try {
                Pong pong = influxDB.ping();
                log.debug("influx new connection ping version={}", pong.getVersion());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            influxDBMap.put(influxDBConnector.hashCode(), influxDB);
        }

        return influxDBMap.get(influxDBConnector.hashCode());
    }

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
            uniqueList = influxDBConnectorList.stream()
                    .collect(Collectors.toMap(
                            connector -> Arrays.asList(connector.getUrl(), connector.getDatabase(), connector.getUsername(), connector.getPassword()),
                            connector -> connector, (existing, replacement) -> existing))
                    .values()
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return uniqueList;
    }

    public List<MeasurementFieldInfo> getMeasurementAndFields(InfluxDBConnector influxDBConnector) {
        String query = String.format("show field keys on %s", influxDBConnector.getDatabase());
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery(query).forDatabase(influxDBConnector.getDatabase());

        InfluxDB db = getDB(influxDBConnector);
        QueryResult qr = db.query(qb.create(), TimeUnit.MILLISECONDS);

        return InfluxDBUtils.measurementAndFieldsMapping(qr.getResults().get(0).getSeries());
    }

    public List<Map<String, Object>> getTags(InfluxDBConnector influxDBConnector) {
        String query = String.format("show tag keys on %s", influxDBConnector.getDatabase());
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery(query).forDatabase(influxDBConnector.getDatabase());

        InfluxDB db = getDB(influxDBConnector);
        QueryResult qr = db.query(qb.create(), TimeUnit.MILLISECONDS);

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

    public Object getMetrics(MetricParamInfo metricParamInfo) {
        InfluxDBConnector influxDBConnector = new InfluxDBConnector(metricParamInfo);

        Long groupSec = getGroupTimeSec(influxDBConnector, metricParamInfo.getMeasurement(), metricParamInfo.getField(), metricParamInfo.getUuid(), metricParamInfo.getRange() + "h");
        if( groupSec < 0 ) {
            return null;
        }
        String queryString = String.format("select time as timestamp, mean(%s) as %s from %s where time > now() - %s and uuid=$uuid group by time(%s), * fill(null) order by time desc limit 30", metricParamInfo.getField(), metricParamInfo.getField(), metricParamInfo.getMeasurement(), metricParamInfo.getRange()+"h", groupSec + "s");
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder
                .newQuery(queryString)
                .forDatabase(influxDBConnector.getDatabase());
        qb.bind("uuid", metricParamInfo.getUuid());

        QueryResult queryResult = getDB(influxDBConnector).query(qb.create());

        List<QueryResult.Result> influxResults = queryResult.getResults();
        List<Map<String, Object>> result = new ArrayList<>();

        if (influxResults.size() > 0 && influxResults.get(0).getSeries() != null) {
            for (int i = 0; i < influxResults.size(); i++) {
                for (int j = 0; j < influxResults.get(i).getSeries().size() ; j++) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("name", influxResults.get(i).getSeries().get(j).getName());
                    dataMap.put("columns", influxResults.get(i).getSeries().get(j).getColumns());
                    dataMap.put("tags", influxResults.get(i).getSeries().get(j).getTags());
                    dataMap.put("values", influxResults.get(i).getSeries().get(j).getValues());
                    result.add(dataMap);
                }
            }
        }

        return result;
    }

    private Long getGroupTimeSec(InfluxDBConnector vo, String measurement, String field, String agentUuid, String shiftTime) {
        String query = String.format("select last(%s) from %s where time >= now() - %s and agent_uuid=$agentUuid; select first(%s) from %s where time >= now() - %s and agent_uuid=$agentUuid", field, measurement, shiftTime, field, measurement, shiftTime);

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery(query).forDatabase(vo.getDatabase());
        qb.bind("agentUuid", agentUuid);
        QueryResult qr = getDB(vo).query(qb.create(), TimeUnit.SECONDS);

        if( qr.hasError() || qr.getResults().get(0).getSeries() == null || qr.getResults().get(1).getSeries() == null ) {
            return -1L;
        }

        Long lastTime = new BigDecimal(qr.getResults().get(0).getSeries().get(0).getValues().get(0).get(0).toString()).longValue();
        Long firstTime = new BigDecimal(qr.getResults().get(1).getSeries().get(0).getValues().get(0).get(0).toString()).longValue();

        return (long) (Math.ceil((lastTime - firstTime) / 30.0));
    }
}
