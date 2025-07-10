package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.history.HistoryDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.history.HistoryListDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.history.HistoryResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HistoryEntity;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.global.definition.TimestampDefinition;
import com.innogrid.tabcloudit.o11ymanager.mapper.history.HistoryMapper;
import com.innogrid.tabcloudit.o11ymanager.mapper.host.HostMapper;
import com.innogrid.tabcloudit.o11ymanager.service.domain.HostDomainService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HistoryService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryFacadeService {

    private final HistoryService historyService;
    private final HostService hostService;
    private final HostMapper hostMapper;
    private final HostDomainService hostDomainService;
    private final HistoryMapper historyMapper;

    @Transactional(readOnly = true)
    public List<HistoryListDTO> list() {
        return historyService.list();
    }

    @Transactional(readOnly = true)
    public HistoryResponseDTO findByIdWithHost(String id) {
        HistoryDTO history = historyService.findHostsById(id);

        HostDTO host = hostService.findById(history.getHostId());

        return HistoryResponseDTO.builder()
                .id(history.getId())
                .timestamp(history.getTimestamp() != null ? history.getTimestamp().format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
                .requestUserId(history.getRequestUserId())
                .isSuccess(history.isSuccess())
                .action(history.getAgentAction())
                .build();
    }

    public List<HistoryResponseDTO> listHistory(String hostId) {

        List<HistoryEntity> historyList = historyService.listHistory(hostId);

        return historyList.stream()
                .sorted(Comparator.comparing(HistoryEntity::getTimestamp).reversed())
                .map(history -> toDTOWithHost(hostId, history))
                .collect(Collectors.toList());
    }



    private HistoryResponseDTO toDTOWithHost(String requestId, HistoryEntity history) {
        HostResponseDTO hostDto;
        try {
            HostEntity host = hostDomainService.getHostById(requestId, history.getHostId());
            hostDto = hostMapper.toDTO(host);
        } catch (Exception ex) {
            hostDto = HostResponseDTO.builder()
                    .id(history.getHostId())
                    .build();
        }
        return historyMapper.toDTO(history, hostDto);
    }


}
