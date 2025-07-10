package com.innogrid.tabcloudit.o11ymanager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "호스트 작업 실행 결과 반환 DTO")
public class HostResultDTO {

    private final String id;

    private boolean isSuccess;
}
