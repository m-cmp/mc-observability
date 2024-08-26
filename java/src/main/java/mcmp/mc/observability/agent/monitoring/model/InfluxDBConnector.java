package mcmp.mc.observability.agent.monitoring.model;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class InfluxDBConnector {
    private InfluxDB influxDB;
    private final String url;
    private final String database;
    private final String retentionPolicy;
    private final String username;
    private final String password;

    private void setInfluxDB() {
        influxDB = (StringUtils.isBlank(username) && StringUtils.isBlank(password))? InfluxDBFactory.connect(url): InfluxDBFactory.connect(url, username, password);
    }

    public InfluxDBConnector(InfluxDBInfo influxDBInfo) {
        this.url = influxDBInfo.getUrl();
        this.database = influxDBInfo.getDatabase();
        this.retentionPolicy = influxDBInfo.getRetentionPolicy();
        this.username = influxDBInfo.getUsername();
        this.password = influxDBInfo.getPassword();
        setInfluxDB();
    }

    public InfluxDBConnector(String setting) {
        setting = setting.replaceAll(" ", "");

        this.url = parseRegex(setting, "(urls=\\[\".*\")\\]", "(urls=)|\\[|\\]|\"");
        this.database = parseRegex(setting, "(database=\".*\")", "(database=)|\\[|\\]|\"");
        this.retentionPolicy = parseRegex(setting, "(retention_policy=\".*\")", "(retention_policy=)|\\[|\\]|\"");
        this.username = parseRegex(setting, "(username=\".*\")", "(username=)|\\[|\\]|\"");
        this.password = parseRegex(setting, "(password=\".*\")", "(password=)|\\[|\\]|\"");
        setInfluxDB();
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
