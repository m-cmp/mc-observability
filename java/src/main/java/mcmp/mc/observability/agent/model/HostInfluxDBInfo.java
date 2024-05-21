package mcmp.mc.observability.agent.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class HostInfluxDBInfo {
    private Long hostSeq;
    private String uuid;
    private List<InfluxDBInfo> influxdbInfos;

    @Data
    public static class InfluxDBInfo {
        private String url;
        private String database;
        private String retentionPolicy;
        private String username;
        private String password;
    }

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

            InfluxDBInfo influxDBInfo = new InfluxDBInfo();
            influxDBInfo.setUrl(extractValue(setting, "urls"));
            influxDBInfo.setDatabase(extractValue(setting, "database"));
            influxDBInfo.setRetentionPolicy(extractValue(setting, "retention_policy"));
            influxDBInfo.setUsername(extractValue(setting, "username"));
            influxDBInfo.setPassword(extractValue(setting, "password"));

            influxDBInfoList.add(influxDBInfo);
        }
        this.influxdbInfos = influxDBInfoList;
    }
    private static String extractValue(String configString, String key) {
        String pattern = key + "\\s*=\\s*(?:\\[\"|\\\")(.+?)(?:\"\\]|\\\")";
        Pattern r = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = r.matcher(configString);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }
}
