package com.mcmp.o11ymanager.oldService.domain;

import com.mcmp.o11ymanager.dto.history.HistoryDTO;
import com.mcmp.o11ymanager.dto.history.HistoryListDTO;
import com.mcmp.o11ymanager.entity.HistoryEntity;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.repository.HistoryJpaRepository;
import com.mcmp.o11ymanager.oldService.domain.interfaces.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// 변환의 책임을 가짐

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

  private final HistoryJpaRepository historyJpaRepository;
  private final RequestInfo requestInfo;

  @Override
  public List<HistoryListDTO> list() {
    return historyJpaRepository.findAll().stream()
        .map(this::toListDTO)
        .toList();
  }

  @Override
  public HistoryDTO findHostsById(String id) {
    HistoryEntity entity = historyJpaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(),
            "History",
            id
        ));

    return HistoryDTO.fromEntity(entity);
  }

  public List<HistoryEntity> listHistory(String hostId) {
    return historyJpaRepository.findByHostId(hostId);
  }


  public HistoryEntity save(HistoryEntity history) {
    return historyJpaRepository.save(history);
  }

  private HistoryListDTO toListDTO(HistoryEntity history) {
    return new HistoryListDTO(
        history.getId(),
        history.getTimestamp() != null ? history.getTimestamp().toString() : null, // 필요하면 포맷 조정
        history.getAgentAction(),
        history.isSuccess(),
        history.getRequestUserId()
    );
  }


}
