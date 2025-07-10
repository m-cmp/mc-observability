package com.innogrid.tabcloudit.o11ymanager.dto.host;

import com.innogrid.tabcloudit.o11ymanager.global.annotation.Base64DecodeField;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.Base64EncodeField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Config 파일 내용 조회를 위한 DTO")
public class ConfigFileContentResponseDTO {

    private String hostId;

    private String commitHash;

    private String path;

    @Base64EncodeField
    @Base64DecodeField
    private String content;
}
