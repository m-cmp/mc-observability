package com.mcmp.o11ymanager.manager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influxdb")
public record InfluxDbInfo(List<Server> servers) {

    public record Server(
            String url,
            String database,
            String retentionPolicy,
            String username,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
            String uid) {

        //    @Override
        //    public String toString() {
        //      return "Server[url=%s, database=%s, username=%s, password=******]"
        //          .formatted(url, database, username);
        //    }
    }
}
