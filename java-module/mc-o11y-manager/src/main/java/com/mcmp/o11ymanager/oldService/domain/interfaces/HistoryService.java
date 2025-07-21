package com.mcmp.o11ymanager.oldService.domain.interfaces;

import com.mcmp.o11ymanager.dto.history.HistoryDTO;
import com.mcmp.o11ymanager.dto.history.HistoryListDTO;
import com.mcmp.o11ymanager.entity.HistoryEntity;

import java.util.List;

public interface HistoryService {

    List<HistoryListDTO> list();

    HistoryDTO findHostsById(String id);

    List<HistoryEntity> listHistory(String hostId);

    HistoryEntity save(HistoryEntity history);
}
