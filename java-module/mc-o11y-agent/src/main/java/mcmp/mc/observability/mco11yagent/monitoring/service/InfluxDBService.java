package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.client.SpiderClient;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.exception.ResultCodeException;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.InfluxDBMapper;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.MiningDBMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.*;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.util.InfluxDBUtils;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static mcmp.mc.observability.mco11yagent.monitoring.enums.Measurement.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBService {

    private final InfluxDBMapper influxDBMapper;
    private final MiningDBMapper miningDBMapper;
    private final MonitoringConfigService monitoringConfigService;
    private final SpiderClient spiderClient;

    public ResBody<List<InfluxDBInfo>> getList() {
        List<MonitoringConfigInfo> storageInfoList = monitoringConfigService.list(null, null, null);
        storageInfoList = storageInfoList.stream().filter(f -> f.getPluginName().equalsIgnoreCase("influxdb")).toList();

        List<InfluxDBInfo> influxDBInfoList = new ArrayList<>();
        for (MonitoringConfigInfo hostStorageInfo : storageInfoList) {
            InfluxDBConnector con = new InfluxDBConnector(hostStorageInfo.getPluginConfig());
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

    public ResBody<List<MeasurementFieldInfo>> getFields() {
        MiningDBInfo miningDBInfo = miningDBMapper.getDetail();

        if( miningDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "miningDB info null");
        }

        InfluxDBInfo influxDBInfo = InfluxDBInfo.builder()
                .url(miningDBInfo.getUrl())
                .database(miningDBInfo.getDatabase())
                .retentionPolicy(miningDBInfo.getRetentionPolicy())
                .username(miningDBInfo.getUsername())
                .password(miningDBInfo.getPassword())
                .build();

        return getFields(influxDBInfo);
    }

    public ResBody<List<MeasurementFieldInfo>> getFields(InfluxDBInfo influxDBInfo) {
        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Can't find configured InfluxDB. InfluxDBInfo is null");
        }

        ResBody<List<MeasurementFieldInfo>> res = new ResBody<>();

        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);

        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery("show field keys").forDatabase(influxDBConnector.getDatabase());

        QueryResult qr = influxDBConnector.getInfluxDB().query(qb.create());

        res.setData(InfluxDBUtils.measurementAndFieldsMapping(qr.getResults().get(0).getSeries()));

        return res;
    }

    public ResBody<List<MeasurementTagInfo>> getTags() {
        MiningDBInfo miningDBInfo = miningDBMapper.getDetail();

        if( miningDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "miningDB info null");
        }

        InfluxDBInfo influxDBInfo = InfluxDBInfo.builder()
                .url(miningDBInfo.getUrl())
                .database(miningDBInfo.getDatabase())
                .retentionPolicy(miningDBInfo.getRetentionPolicy())
                .username(miningDBInfo.getUsername())
                .password(miningDBInfo.getPassword())
                .build();

        return getTags(influxDBInfo);
    }

    public ResBody<List<MeasurementTagInfo>> getTags(InfluxDBInfo influxDBInfo) {
        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Can't find configured InfluxDB. InfluxDBInfo is null");
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
        MiningDBInfo miningDBInfo = miningDBMapper.getDetail();

        if( miningDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "miningDB info null");
        }

        InfluxDBInfo influxDBInfo = InfluxDBInfo.builder()
                .url(miningDBInfo.getUrl())
                .database(miningDBInfo.getDatabase())
                .retentionPolicy(miningDBInfo.getRetentionPolicy())
                .username(miningDBInfo.getUsername())
                .password(miningDBInfo.getPassword())
                .build();

        return getMetrics(influxDBInfo, metricsInfo);
    }

    public List<MetricInfo> getMetrics(InfluxDBInfo influxDBInfo, MetricsInfo metricsInfo) {
        if( influxDBInfo == null ) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Can't find configured InfluxDB. InfluxDBInfo is null");
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

    private void writeCPU(TumblebugMCI.Vm vm, InfluxDBConnector influxDBConnector, String timeBeforeHour, String intervalMinute) {
        SpiderMonitoringInfo.Data data = spiderClient.getVMMonitoring(vm.getCspResourceName(), CPU_USAGE.toString(), vm.getConnectionName(), timeBeforeHour, intervalMinute);

        for (SpiderMonitoringInfo.Data.TimestampValue timestampValue : data.getTimestampValues()) {
            String timestampString = timestampValue.getTimestamp();
            String valueString = timestampValue.getValue();

            long timestamp = TimeUnit.MILLISECONDS.toSeconds(
                    Instant.parse(timestampString).toEpochMilli()
            );
            double value = Double.parseDouble(valueString);

            Point point = Point.measurement("cpu")
                    .time(timestamp, TimeUnit.SECONDS)
                    .addField("cpu", "cpu-total")
                    .addField("usage", value)
                    .build();
            influxDBConnector.getInfluxDB().write(point);
            influxDBConnector.getInfluxDB().close();
        }
    }

    private void writeDiskIO(TumblebugMCI.Vm vm, InfluxDBConnector influxDBConnector, String timeBeforeHour, String intervalMinute) {
        SpiderMonitoringInfo.Data dataReadBytes = spiderClient.getVMMonitoring(vm.getCspResourceName(), DISK_READ.toString(), vm.getConnectionName(), timeBeforeHour, intervalMinute);
        SpiderMonitoringInfo.Data dataWriteBytes = spiderClient.getVMMonitoring(vm.getCspResourceName(), DISK_WRITE.toString(), vm.getConnectionName(), timeBeforeHour, intervalMinute);

        for (SpiderMonitoringInfo.Data.TimestampValue timestampValueReadBytes : dataReadBytes.getTimestampValues()) {
            for (SpiderMonitoringInfo.Data.TimestampValue timestampValueWriteBytes : dataWriteBytes.getTimestampValues()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                Instant instantWriteBytes = Instant.parse(timestampValueReadBytes.getTimestamp());
                ZonedDateTime dateTimeWriteBytes = instantWriteBytes.atZone(ZoneId.of("UTC"));
                String formattedDateWriteBytes = dateTimeWriteBytes.format(formatter);

                Instant instantReadBytes = Instant.parse(timestampValueReadBytes.getTimestamp());
                ZonedDateTime dateTimeReadBytes = instantReadBytes.atZone(ZoneId.of("UTC"));
                String formattedDateReadBytes = dateTimeReadBytes.format(formatter);

                if (formattedDateWriteBytes.equals(formattedDateReadBytes)) {
                    String timestampString = timestampValueReadBytes.getTimestamp();
                    String readBytesValueString = timestampValueReadBytes.getValue();
                    String writeBytesValueString = timestampValueWriteBytes.getValue();

                    long timestamp = TimeUnit.MILLISECONDS.toSeconds(
                            Instant.parse(timestampString).toEpochMilli()
                    );
                    double readBytesValue = Double.parseDouble(readBytesValueString);
                    double writeBytesValue = Double.parseDouble(writeBytesValueString);

                    Point point = Point.measurement("disk")
                            .time(timestamp, TimeUnit.SECONDS)
                            .addField("read_bytes", readBytesValue)
                            .addField("write_bytes", writeBytesValue)
                            .build();
                    influxDBConnector.getInfluxDB().write(point);

                    break;
                }
            }
        }
        influxDBConnector.getInfluxDB().close();
    }

    private void writeMem(TumblebugMCI.Vm vm, InfluxDBConnector influxDBConnector, String timeBeforeHour, String intervalMinute) {
        SpiderMonitoringInfo.Data data = spiderClient.getVMMonitoring(vm.getCspResourceName(), MEMORY_USAGE.toString(), vm.getConnectionName(), timeBeforeHour, intervalMinute);

        for (SpiderMonitoringInfo.Data.TimestampValue timestampValue : data.getTimestampValues()) {
            String timestampString = timestampValue.getTimestamp();
            String valueString = timestampValue.getValue();

            long timestamp = TimeUnit.MILLISECONDS.toSeconds(
                    Instant.parse(timestampString).toEpochMilli()
            );
            double value = Double.parseDouble(valueString);

            Point point = Point.measurement("mem")
                    .time(timestamp, TimeUnit.SECONDS)
                    .addField("used_percent", value)
                    .build();
            influxDBConnector.getInfluxDB().write(point);
            influxDBConnector.getInfluxDB().close();
        }
    }

    public void writeMetrics(TumblebugMCI.Vm vm, InfluxDBInfo influxDBInfo, String pluginName, String timeBeforeHour, String intervalMinute) {
        if (influxDBInfo == null) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "influxDB info is null ");
        }

        InfluxDBConnector influxDBConnector = new InfluxDBConnector(influxDBInfo);


        switch (pluginName) {
            case "cpu" -> writeCPU(vm, influxDBConnector, timeBeforeHour, intervalMinute);
            case "diskio" -> writeDiskIO(vm, influxDBConnector, timeBeforeHour, intervalMinute);
            case "mem" -> writeMem(vm, influxDBConnector, timeBeforeHour, intervalMinute);
        }
    }
}
