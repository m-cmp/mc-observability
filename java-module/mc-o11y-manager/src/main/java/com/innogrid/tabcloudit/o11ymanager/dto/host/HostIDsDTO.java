package com.innogrid.tabcloudit.o11ymanager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "호스트 관련 작업 요청을 위한 DTO")
public class HostIDsDTO {
    @Schema(description = "호스트 ID 리스트", example = "[\"51cbd96c-797d-4ff1-a31b-1730ff5be0d2\", \"94c3e402-5042-4c8e-9248-cdfcb1462deb\"]")
    @NotBlank
    private String[] host_id_list;
}
