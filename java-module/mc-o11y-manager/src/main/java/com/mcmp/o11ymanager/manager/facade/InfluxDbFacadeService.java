package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDbFacadeService {

    private final InfluxDbService influxDbService;

    @Transactional(readOnly = true)
    public InfluxDTO resolveForVM(String nsId, String mciId) {
        return influxDbService.resolveInfluxDto(nsId, mciId);
    }

    public List<TagDTO> getTags() {
        return influxDbService.getTags().getData();
    }

    public List<FieldDTO> getFields() {
        return influxDbService.getFields().getData();
    }

    public List<MetricDTO> getMetrics(String nsId, String mciId, MetricRequestDTO req) {
        return influxDbService.getMetrics(nsId, mciId, req);
    }

    public List<InfluxDTO> getInfluxDbs() {
        return influxDbService.rawServers();
    }
}
