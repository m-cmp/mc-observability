package com.mcmp.o11ymanager.manager.mapper.host;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import org.springframework.stereotype.Component;

@Component
public class VMMapper {

    public VMDTO toDTO(VMEntity vm) {
        return VMDTO.fromEntity(vm);
    }

    public VMEntity fromResponseDTO(VMDTO dto) {
        return VMEntity.builder()
                .vmId(dto.getVmId())
                .nsId(dto.getNsId())
                .vmStatus(dto.getVmStatus())
                .mciId(dto.getMciId())
                .name(dto.getName())
                .description(dto.getDescription())
                .monitoringAgentTaskStatus(dto.getMonitoringAgentTaskStatus())
                .logAgentTaskStatus(dto.getLogAgentTaskStatus())
                .vmMonitoringAgentTaskId(dto.getVmMonitoringAgentTaskId())
                .vmLogAgentTaskId(dto.getVmLogAgentTaskId())
                .monitoringServiceStatus(dto.getMonitoringServiceStatus())
                .logServiceStatus(dto.getLogServiceStatus())
                .nsId(dto.getNsId())
                .mciId(dto.getMciId())
                .build();
    }

    public VMEntity fromCreateDTO(VMRequestDTO dto) {
        return VMEntity.builder().name(dto.getName()).description(dto.getDescription()).build();
    }

    public void fromUpdateDTO(VMRequestDTO dto, VMEntity vm) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            vm.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            vm.setDescription(dto.getDescription());
        }
    }
}
