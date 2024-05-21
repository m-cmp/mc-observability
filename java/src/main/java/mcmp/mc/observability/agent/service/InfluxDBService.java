package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.util.InfluxDBUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Pong;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

        return InfluxDBUtils.metricMapping(qr.getResults().get(0).getSeries());
    }

}
