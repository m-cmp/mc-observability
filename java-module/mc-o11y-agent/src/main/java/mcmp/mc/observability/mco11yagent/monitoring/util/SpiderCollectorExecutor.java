package mcmp.mc.observability.mco11yagent.monitoring.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.MetricType;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBInfo;
import mcmp.mc.observability.mco11yagent.monitoring.service.InfluxDBService;
import mcmp.mc.observability.mco11yagent.monitoring.service.MonitoringConfigService;
import mcmp.mc.observability.mco11yagent.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderCollectorExecutor {
    private final MonitoringConfigService monitoringConfigService;
    private Process SPIDER_COLLECTOR_PROCESS = null;

    private final InfluxDBService influxDBService;
    private final MonitoringService monitoringService;

    @Value("${feign.cb-spider.monitoring.influxdb_url}")
    private String influxdb_url;
    @Value("${feign.cb-spider.monitoring.influxdb_database}")
    private String influxdb_database;
    @Value("${feign.cb-spider.monitoring.influxdb_retention_policy}")
    private String influxdb_retention_policy;
    @Value("${feign.cb-spider.monitoring.influxdb_username}")
    private String influxdb_username;
    @Value("${feign.cb-spider.monitoring.influxdb_password}")
    private String influxdb_password;

    @PreDestroy
    public void stopSpiderCollector() {
        if( SPIDER_COLLECTOR_PROCESS == null ) return;
        SPIDER_COLLECTOR_PROCESS.destroy();
    }

    public void startSpiderCollector() {
        if (SPIDER_COLLECTOR_PROCESS == null || !SPIDER_COLLECTOR_PROCESS.isAlive()) {
            String nsId = System.getenv(Constants.PROPERTY_NS_ID);
            if( nsId == null ) nsId = "";
            String mciId = System.getenv(Constants.PROPERTY_MCI_ID);
            if( mciId == null ) mciId = "";
            String targetId = System.getenv(Constants.PROPERTY_TARGET_ID);
            if( targetId == null ) targetId = "";

            InfluxDBInfo influxDBinfo = InfluxDBInfo.builder()
                    .url(influxdb_url)
                    .database(influxdb_database)
                    .retentionPolicy(influxdb_retention_policy)
                    .username(influxdb_username)
                    .password(influxdb_password)
                    .build();
            monitoringService.writeSpiderVMMonitoring(influxDBinfo, nsId, mciId, targetId, "1", "1");
        }
    }

    public boolean isSpiderCollectorAlive() {
        return SPIDER_COLLECTOR_PROCESS != null && SPIDER_COLLECTOR_PROCESS.isAlive();
    }
}
