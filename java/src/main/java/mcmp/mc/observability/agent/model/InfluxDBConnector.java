package mcmp.mc.observability.agent.model;

import lombok.Getter;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class InfluxDBConnector {
    private InfluxDB influxDB;
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;

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
