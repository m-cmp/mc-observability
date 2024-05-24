package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class InfluxDBConnector {
    @JsonIgnore
    private InfluxDB influxDB;
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;

    public InfluxDBConnector() {}

    public InfluxDBConnector(MetricParamInfo metricParamInfo) {
        this.url = metricParamInfo.getUrl();
        this.database = metricParamInfo.getDatabase();
        this.retentionPolicy = metricParamInfo.getRetentionPolicy();
        this.username = metricParamInfo.getUsername();
        this.password = metricParamInfo.getPassword();
    }

    public InfluxDBConnector(MetricDataParamInfo metricDataParamInfo) {
        this.url = metricDataParamInfo.getUrl();
        this.database = metricDataParamInfo.getDatabase();
        this.retentionPolicy = metricDataParamInfo.getRetentionPolicy();
        this.username = metricDataParamInfo.getUsername();
        this.password = metricDataParamInfo.getPassword();
    }

    public InfluxDBConnector(String setting) {
        setting = setting.replaceAll(" ", "");

        this.url = parseRegex(setting, "(urls=\\[\".*\")\\]", "(urls=)|\\[|\\]|\"");
        this.database = parseRegex(setting, "(database=\".*\")", "(database=)|\\[|\\]|\"");
        this.retentionPolicy = parseRegex(setting, "(retention_policy=\".*\")", "(retention_policy=)|\\[|\\]|\"");
        this.username = parseRegex(setting, "(username=\".*\")", "(username=)|\\[|\\]|\"");
        this.password = parseRegex(setting, "(password=\".*\")", "(password=)|\\[|\\]|\"");

        influxDB = (username.isEmpty() && password.isEmpty())? InfluxDBFactory.connect(url): InfluxDBFactory.connect(url, username, password);
    }

    private String parseRegex(String origin, String findRegex, String replaceRegex) {
        Pattern p = Pattern.compile(findRegex);
        Matcher m = p.matcher(origin);

        if( m.find() ) {
            String findStr = m.group();
            return findStr.replaceAll(replaceRegex, "");
        }

        return "";
    }
}
