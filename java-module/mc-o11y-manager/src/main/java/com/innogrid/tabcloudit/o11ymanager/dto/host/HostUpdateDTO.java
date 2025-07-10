package com.innogrid.tabcloudit.o11ymanager.dto.host;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
@Schema(description = "호스트 업데이트를 위한 DTO")
public class HostUpdateDTO {

    @Schema(description = "호스트 ID 리스트", example = "[\"51cbd96c-797d-4ff1-a31b-1730ff5be0d2\", \"94c3e402-5042-4c8e-9248-cdfcb1462deb\"]")
    @NotEmpty
    private List<String> host_id_list;

    @Schema(description = "모든 호스트에 적용할 변경 정보")
    @NotNull
    private UpdateData data;

    @Getter
    @Setter
    public static class UpdateData {

        @Schema(description = "호스트 포트", example = "22")
        private Integer port;

        @Schema(description = "호스트 계정 이름", example = "root")
        private String user;

        @Schema(description = "호스트 계정 비밀번호", example = "qwe1212!Q")
        private String password;
    }
}
