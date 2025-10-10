package com.mcmp.o11ymanager.manager.dto.vm;

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

  @NotBlank(message = "VM name is required")
  private String name;

  private String description;
}
