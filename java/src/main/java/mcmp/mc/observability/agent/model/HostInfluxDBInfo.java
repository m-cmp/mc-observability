package mcmp.mc.observability.agent.model;

import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class HostInfluxDBInfo {
    private Long hostSeq;
    private String uuid;
    private List<InfluxDBInfo> influxdbInfos;

    public HostInfluxDBInfo(Long hostSeq, String uuid) {
        this.hostSeq = hostSeq;
        this.uuid = uuid;
    }

    public void mappingInfluxDbInfo(List<HostStorageInfo> storageInfoList) {
        if(CollectionUtils.isEmpty(storageInfoList))
            return;

        List<InfluxDBInfo> influxDBInfoList = new ArrayList<>();
        for(HostStorageInfo storageInfo : storageInfoList) {
            String setting = storageInfo.getSetting();

            InfluxDBInfo influxDBInfo = InfluxDBInfo.builder()
                    .url(StringUtils.extractConfigValue(setting, "urls"))
                    .database(StringUtils.extractConfigValue(setting, "database"))
                    .retentionPolicy(StringUtils.extractConfigValue(setting, "retention_policy"))
                    .username(StringUtils.extractConfigValue(setting, "username"))
                    .password(StringUtils.extractConfigValue(setting, "password"))
                    .build();

            influxDBInfoList.add(influxDBInfo);
        }
        this.influxdbInfos = influxDBInfoList;
    }
}
