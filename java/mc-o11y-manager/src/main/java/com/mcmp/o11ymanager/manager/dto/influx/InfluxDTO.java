package com.mcmp.o11ymanager.manager.dto.influx;

import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "InfluxDB ID", example = "0")
    private Long id;

    @Schema(description = "InfluxDB URL", example = "mcmp:8086")
    private String url;

    @Schema(description = "Database name", example = "mydb")
    private String database;

    @Schema(description = "Database username", example = "mc-user")
    private String username;

    @Schema(description = "Retention policy", example = "autogen")
    private String retention_policy;

    @Schema(description = "Database password", example = "mypw")
    private String password;

    @Schema(description = "Unique identifier", example = "sdfsj1df33ff")
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
