package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.model.LogsInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.OpenSearchInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.OpenSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI + "/opensearch")
public class OpenSearchController {

    private final OpenSearchService opensearchService;

    @GetMapping
    public ResBody<List<OpenSearchInfo>> list() {
        return opensearchService.getList();
    }

    @PostMapping("/logs")
    public ResBody<List<Map<String, Object>>> metric(@RequestBody LogsInfo logsInfo) {
        ResBody<List<Map<String, Object>>> resBody = new ResBody<>();
        resBody.setData(opensearchService.getLogs(logsInfo));
        return resBody;
    }
}
