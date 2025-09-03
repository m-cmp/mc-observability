package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 응용 계층에서 사용하는 레이블 응답 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponseDto {
    private String status;
    private List<String> labels;
}
