package com.mcmp.o11ymanager.manager.infrastructure.log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Loki 레이블 API 응답을 매핑하는 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LokiLabelsResponseDto {
    private String status;
    private List<String> data;
}
