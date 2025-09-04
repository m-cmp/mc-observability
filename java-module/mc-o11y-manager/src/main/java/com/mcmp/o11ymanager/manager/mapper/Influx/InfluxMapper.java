package com.mcmp.o11ymanager.manager.mapper.Influx;

import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.entity.InfluxDbInfo.Server;
import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InfluxMapper {

    public InfluxEntity toEntity(Server server) {
        return InfluxEntity.builder()
                .url(server.url())
                .database(server.database())
                .retentionPolicy(server.retentionPolicy())
                .username(server.username())
                .password(server.password())
                .uid(server.uid())
                .build();
    }

    public List<InfluxEntity> toEntities(List<Server> servers) {
        return servers.stream().map(s -> toEntity(s)).toList();
    }

    public InfluxEntity toEntity(InfluxDTO dto, String uid) {
        return InfluxEntity.builder()
                .url(dto.getUrl())
                .database(dto.getDatabase())
                .retentionPolicy(dto.getRetention_policy()) // snake_case 그대로 매핑
                .username(dto.getUsername())
                .password(dto.getPassword())
                .uid(uid)
                .build();
    }

    public InfluxEntity toEntity(InfluxDTO dto) {
        return toEntity(dto);
    }

    public List<InfluxEntity> toEntitiesFromDtos(List<InfluxDTO> dtos) {
        return dtos.stream().map(d -> toEntity(d)).toList();
    }

    public void merge(InfluxEntity target, InfluxDTO source) {
        if (source.getUrl() != null) target.setUrl(source.getUrl());
        if (source.getDatabase() != null) target.setDatabase(source.getDatabase());
        if (source.getRetention_policy() != null)
            target.setRetentionPolicy(source.getRetention_policy());
        if (source.getUsername() != null) target.setUsername(source.getUsername());
        if (source.getPassword() != null) target.setPassword(source.getPassword());
    }
}
