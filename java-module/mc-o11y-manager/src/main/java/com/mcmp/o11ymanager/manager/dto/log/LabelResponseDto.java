package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Label response DTO used in the application layer */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponseDto {
    private String status;
    private List<String> labels;
}
