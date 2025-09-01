package com.mcmp.o11ymanager.manager.infrastructure.log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Loki 레이블 API 응답을 매핑하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LokiLabelsResponseDto {
    private String status;
    private List<String> data;
} 