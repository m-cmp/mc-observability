package com.mcmp.o11ymanager.manager.dto.vm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class VMRequestDTO {

    @Schema(description = "VM name", example = "mcmp-vm")
    @NotBlank(message = "VM name is required")
    private String name;

    @Schema(description = "VM description", example = "string")
    private String description;
}
