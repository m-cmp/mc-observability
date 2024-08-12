package mcmp.mc.observability.agent.monitoring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class HostInfluxDBInfo {
    @ApiModelProperty(value = "Sequence by host")
    private Long hostSeq;
    @ApiModelProperty(value = "Uuid by host")
    private String uuid;
    @ApiModelProperty(value = "Influxdb configuration information")
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
