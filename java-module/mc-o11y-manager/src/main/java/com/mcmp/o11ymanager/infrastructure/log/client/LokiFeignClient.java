package com.mcmp.o11ymanager.infrastructure.log.client;

import com.mcmp.o11ymanager.infrastructure.log.dto.LokiLabelsResponseDto;
import com.mcmp.o11ymanager.infrastructure.log.dto.LokiResponseDto;
import com.mcmp.o11ymanager.infrastructure.log.dto.LokiVolumeResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@FeignClient(
        name = "LokiFeignClient",
        url = "${loki.url}",
        configuration = FeignLogConfig.class
)
public interface LokiFeignClient {

    @GetMapping(value = "${loki.endpoints.query}")
    Optional<LokiResponseDto> fetchLogs(
            @RequestParam String query,
            @RequestParam int limit);

    @GetMapping(value = "${loki.endpoints.range}")
    Optional<LokiResponseDto> fetchLogsWithRange(
            @RequestParam String query,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam Integer limit,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String since);

    @GetMapping(value = "/loki/api/v1/query_range")
    Optional<LokiVolumeResponseDto> fetchLogVolumes(
            @RequestParam String query,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Integer limit);

    @GetMapping(value = "/loki/api/v1/labels")
    Optional<LokiLabelsResponseDto> fetchLabels(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String query);

    @GetMapping(value = "/loki/api/v1/label/{label}/values")
    Optional<LokiLabelsResponseDto> fetchLabelValues(
            @PathVariable String label,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String query);
}
