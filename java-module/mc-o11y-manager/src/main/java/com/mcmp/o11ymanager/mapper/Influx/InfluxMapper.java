package com.mcmp.o11ymanager.mapper.Influx;


import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.UidDTO;
import com.mcmp.o11ymanager.entity.InfluxDbInfo;
import com.mcmp.o11ymanager.entity.InfluxDbInfo.Server;
import com.mcmp.o11ymanager.entity.InfluxEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InfluxMapper {

  public InfluxEntity toEntity(InfluxDbInfo.Server server, String uid) {
    return InfluxEntity.builder()
        .url(server.url())
        .database(server.database())
        .retentionPolicy(server.retentionPolicy())
        .username(server.username())
        .password(server.password())
        .uid(uid)
        .build();
  }

  public InfluxEntity toEntity(InfluxDbInfo.Server server, UidDTO dto) {
    return toEntity(server, dto.getUid());
  }

  public List<InfluxEntity> toEntities(List<Server> servers, String uid) {
    return servers.stream()
        .map(s -> toEntity(s, uid))
        .toList();
  }

  public List<InfluxEntity> toEntities(List<InfluxDbInfo.Server> servers, UidDTO dto) {
    return toEntities(servers, dto.getUid());
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

  public InfluxEntity toEntity(InfluxDTO dto, UidDTO uidDto) {
    return toEntity(dto, uidDto.getUid());
  }

  public List<InfluxEntity> toEntitiesFromDtos(List<InfluxDTO> dtos, String uid) {
    return dtos.stream()
        .map(d -> toEntity(d, uid))
        .toList();
  }

  public List<InfluxEntity> toEntitiesFromDtos(List<InfluxDTO> dtos, UidDTO uidDto) {
    return dtos.stream()
        .map(d -> toEntity(d, uidDto))
        .toList();
  }

  public void merge(InfluxEntity target, InfluxDTO source) {
    if (source.getUrl() != null) target.setUrl(source.getUrl());
    if (source.getDatabase() != null) target.setDatabase(source.getDatabase());
    if (source.getRetention_policy() != null) target.setRetentionPolicy(source.getRetention_policy());
    if (source.getUsername() != null) target.setUsername(source.getUsername());
    if (source.getPassword() != null) target.setPassword(source.getPassword());
  }


}
