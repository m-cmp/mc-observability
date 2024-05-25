package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.mapper.InfluxDBMapper;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.model.InfluxDBInfo;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.model.MetricInfo;
import mcmp.mc.observability.agent.model.MetricsInfo;
import mcmp.mc.observability.agent.util.InfluxDBUtils;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBService {
    private final InfluxDBMapper influxDBMapper;
    private final HostStorageService hostStorageService;

    public List<InfluxDBInfo> getList() {

        List<HostStorageInfo> storageInfoList = hostStorageService.getAllList();
        if (CollectionUtils.isEmpty(storageInfoList))
            return Collections.emptyList();

        List<InfluxDBInfo> influxDBInfoList = new ArrayList<>();
        for (HostStorageInfo hostStorageInfo : storageInfoList) {
            InfluxDBConnector con = new InfluxDBConnector(hostStorageInfo.getSetting());
            InfluxDBInfo influxDBinfo = InfluxDBInfo.builder()
                    .url(con.getUrl())
                    .database(con.getDatabase())
                    .retentionPolicy(con.getRetentionPolicy())
                    .username(con.getUsername())
                    .password(con.getPassword())
                    .build();

            influxDBInfoList.add(influxDBinfo);
        }

        syncSummaryInfluxDBList(influxDBInfoList.stream().distinct().collect(Collectors.toList()));

        return influxDBMapper.getInfluxDBInfoList();
    }

    private void syncSummaryInfluxDBList(List<InfluxDBInfo> influxDBInfoList) {
        Map<Long, InfluxDBInfo> summaryInfluxDBInfoList = influxDBMapper.getInfluxDBInfoMap();
        List<InfluxDBInfo> newList = new ArrayList<>();
        List<Long> delList = new ArrayList<>();

        for( InfluxDBInfo info : influxDBInfoList ) {
            Optional<Map.Entry<Long, InfluxDBInfo>> findEntry = summaryInfluxDBInfoList.entrySet().stream().filter(a -> a.getValue().hashCode() == info.hashCode()).findAny();
            if (findEntry.isPresent()) {
                summaryInfluxDBInfoList.remove(findEntry.get().getKey());
            } else {
                newList.add(info);
            }
        }

        summaryInfluxDBInfoList.forEach((key, value) -> delList.add(key));

        if( !newList.isEmpty() ) influxDBMapper.insertInfluxDBInfoList(newList);
        if( !delList.isEmpty() ) influxDBMapper.deleteInfluxDBInfoList(delList);

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
            return Collections.emptyList();

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

    public List<MetricInfo> getMetrics(MetricsInfo metricsInfo) {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfo(metricsInfo.getInfluxDBSeq());
        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);
        String queryString = metricsInfo.getQuery();

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
