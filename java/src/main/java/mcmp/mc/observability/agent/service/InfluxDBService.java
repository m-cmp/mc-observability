package mcmp.mc.observability.agent.service;

import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.util.InfluxDBUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Pong;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InfluxDBService {
    private static final Map<Integer, InfluxDB> influxDBMap = new HashMap<>();

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

    public List<MeasurementFieldInfo> getMeasurementAndFields(InfluxDBConnector influxDBConnector) {
        String query = String.format("show field keys on %s", influxDBConnector.getDatabase());
        BoundParameterQuery.QueryBuilder qb = BoundParameterQuery.QueryBuilder.newQuery(query).forDatabase(influxDBConnector.getDatabase());

        InfluxDB db = getDB(influxDBConnector);
        QueryResult qr = db.query(qb.create(), TimeUnit.MILLISECONDS);

        return InfluxDBUtils.metricMapping(qr.getResults().get(0).getSeries());
    }

}
