package com.mcmp.o11ymanager.manager.dto.influx;

import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluxDTO {

    private Long id;

    private String url;

    private String database;

    private String username;

    private String retention_policy;

    private String password;

    private String uid;

    public static InfluxDTO fromEntity(InfluxEntity entity) {
        return InfluxDTO.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .database(entity.getDatabase())
                .username(entity.getUsername())
                .retention_policy(entity.getRetentionPolicy())
                .password(entity.getPassword())
                .uid(entity.getUid())
                .build();
    }

    public InfluxEntity toEntity() {
        return InfluxEntity.builder()
                .id(this.id)
                .url(this.url)
                .database(this.database)
                .username(this.username)
                .password(this.password)
                .uid(this.uid)
                .retentionPolicy(this.retention_policy)
                .build();
    }
}
