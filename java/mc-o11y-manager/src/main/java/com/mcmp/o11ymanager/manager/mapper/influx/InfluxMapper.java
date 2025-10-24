package com.mcmp.o11ymanager.manager.mapper.influx;

import com.mcmp.o11ymanager.manager.entity.InfluxDbInfo.Server;
import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import org.springframework.stereotype.Component;

@Component
public class InfluxMapper {

    public InfluxEntity toEntity(Server server, String database, String retentionPolicy) {
        return InfluxEntity.builder()
                .url(server.url())
                .database(database)
                .retentionPolicy(retentionPolicy)
                .username(server.username())
                .password(server.password())
                .uid(server.uid())
                .build();
    }
}
