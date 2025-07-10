package com.innogrid.tabcloudit.o11ymanager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Config 파일/디렉토리 목록 조회를 위한 DTO")
public class ConfigFileDTO {
    @Schema(description = "파일/디렉토리 이름", example = "telegraf.conf")
    private String name;

    @Schema(description = "파일/디렉토리 경로", example = "telegraf.conf")
    private String path;

    @Schema(description = "디렉토리 인지 여부", example = "false")
    private boolean isDirectory;

    @Schema(description = "디렉토리일 경우의 하위 파일/디렉토리 리스트", example = "[\"001.conf\", \"002.conf\"]")
    private List<ConfigFileDTO> children;
}
