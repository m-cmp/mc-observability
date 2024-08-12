package mcmp.mc.observability.agent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.monitoring.mapper.InfluxDBMapper;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBConnector;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBInfo;
import mcmp.mc.observability.agent.monitoring.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.monitoring.model.MeasurementTagInfo;
import mcmp.mc.observability.agent.monitoring.model.MetricInfo;
import mcmp.mc.observability.agent.monitoring.model.MetricsInfo;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.common.util.InfluxDBUtils;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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

    public ResBody<List<InfluxDBInfo>> getList() {

        List<HostStorageInfo> storageInfoList = hostStorageService.getAllList();

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

        ResBody<List<InfluxDBInfo>> res = new ResBody<>();
        res.setData(influxDBMapper.getInfluxDBInfoList());

        return res;
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

    public ResBody<List<MeasurementFieldInfo>> getFields(Long influxDBSeq) {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfo(influxDBSeq);

        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "influxDB info null seq = {}", influxDBSeq);
        }

        ResBody<List<MeasurementFieldInfo>> res = new ResBody<>();

        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery("show field keys").forDatabase(influxDBConnector.getDatabase());

        QueryResult qr = influxDBConnector.getInfluxDB().query(qb.create());

        res.setData(InfluxDBUtils.measurementAndFieldsMapping(qr.getResults().get(0).getSeries()));

        return res;
    }

    public ResBody<List<MeasurementTagInfo>> getTags(Long influxDBSeq) {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfo(influxDBSeq);

        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "influxDB info null seq = {}", influxDBSeq);
        }

        ResBody<List<MeasurementTagInfo>> res = new ResBody<>();

        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery("show tag keys").forDatabase(influxDBConnector.getDatabase());

        QueryResult qr = influxDBConnector.getInfluxDB().query(qb.create());

        List<MeasurementTagInfo> result = new ArrayList<>();
        List<QueryResult.Series> seriesList = qr.getResults().get(0).getSeries();

        if(CollectionUtils.isEmpty(seriesList))
            return res;

        for( QueryResult.Series series : seriesList) {
            List<String> tagList = new ArrayList<>();
            series.getValues().forEach(f -> tagList.add(f.get(0).toString()));

            result.add(MeasurementTagInfo.builder()
                    .measurement(series.getName())
                    .tags(tagList)
                    .build());
        }

        res.setData(result);

        return res;
    }

    public List<MetricInfo> getMetrics(MetricsInfo metricsInfo) {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfo(metricsInfo.getInfluxDBSeq());

        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "influxDB info null seq = {}", metricsInfo.getInfluxDBSeq());
        }

        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);
        String queryString = metricsInfo.getQuery();

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder
                .newQuery(queryString)
                .forDatabase(influxDBConnector.getDatabase());

        QueryResult queryResult = influxDBConnector.getInfluxDB().query(qb.create());

        List<QueryResult.Result> influxResults = queryResult.getResults();
        List<MetricInfo> result = new ArrayList<>();

        if( influxResults.isEmpty() || influxResults.get(0).getSeries() == null ) {
            return result;
        }

        influxResults.forEach(f -> f.getSeries().forEach(f2 -> result.add(MetricInfo.builder().name(f2.getName()).columns(f2.getColumns()).tags(f2.getTags()).values(f2.getValues()).build())));

        return result;
    }
}
