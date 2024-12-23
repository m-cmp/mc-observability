package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.model.dto.MonitoringConfigInfoCreateDTO;
import mcmp.mc.observability.mco11ymanager.model.dto.MonitoringConfigInfoUpdateDTO;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import mcmp.mc.observability.mco11ymanager.model.*;
import mcmp.mc.observability.mco11ymanager.model.dto.ResBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "agent-manager", url = "${feign.agent-manager.url:}")
public interface MonitoringClient {

    @GetMapping(Constants.PREFIX_V1 + "/monitoring/plugins")
    ResBody<List<PluginDefInfo>> getPluginList();

    @GetMapping(Constants.PREFIX_V1 + "/monitoring/target")
    ResBody<List<TargetInfo>> getTargetList();
    @GetMapping(Constants.PREFIX_V1 + "/monitoring/{nsId}/{mciId}/target")
    ResBody<List<TargetInfo>> getTargetListNSMCI(@PathVariable("nsId") String nsId, @PathVariable("mciId") String mciId);
    @GetMapping(Constants.PREFIX_V1 + Constants.TARGET_PATH)
    ResBody<TargetInfo> getTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId);
    @PostMapping(Constants.PREFIX_V1 + Constants.TARGET_PATH)
    ResBody insertTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object targetInfo);
    @PutMapping(Constants.PREFIX_V1 + Constants.TARGET_PATH)
    ResBody updateTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object targetInfo);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TARGET_PATH)
    ResBody deleteTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId);

    @GetMapping(Constants.PREFIX_V1 + Constants.TARGET_CSP_PATH)
    ResBody<SpiderMonitoringInfo.Data> getCSP(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable String measurement);

    @GetMapping(Constants.PREFIX_V1 + Constants.TARGET_ITEM_PATH)
    ResBody<List<MonitoringConfigInfo>> getItemList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId);
    @PostMapping(Constants.PREFIX_V1 + Constants.TARGET_ITEM_PATH)
    ResBody insertItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoCreateDTO itemCreateInfo);
    @PutMapping(Constants.PREFIX_V1 + Constants.TARGET_ITEM_PATH)
    ResBody updateItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoUpdateDTO itemUpdateInfo);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TARGET_ITEM_PATH + "/{itemSeq}")
    ResBody deleteItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long itemSeq);

    @GetMapping(Constants.PREFIX_V1 + Constants.TARGET_STORAGE_PATH)
    ResBody<List<MonitoringConfigInfo>> getStorageList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId);
    @PostMapping(Constants.PREFIX_V1 + Constants.TARGET_STORAGE_PATH)
    ResBody insertStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoCreateDTO storageCreateInfo);
    @PutMapping(Constants.PREFIX_V1 + Constants.TARGET_STORAGE_PATH)
    ResBody updateStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoUpdateDTO storageUpdateInfo);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TARGET_STORAGE_PATH + "/{storageSeq}")
    ResBody deleteStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long storageSeq);

    @GetMapping(Constants.PREFIX_V1 + Constants.INFLUXDB_PATH)
    ResBody<List<InfluxDBInfo>> getInfluxDBList();
    @GetMapping(Constants.PREFIX_V1 + Constants.INFLUXDB_PATH + "/measurement")
    ResBody<List<MeasurementFieldInfo>> getInfluxDBFields();
    @GetMapping(Constants.PREFIX_V1 + Constants.INFLUXDB_PATH + "/tag")
    ResBody<List<MeasurementTagInfo>> getInfluxDBTags();
    @PostMapping(Constants.PREFIX_V1 + Constants.INFLUXDB_PATH + "/metric")
    ResBody<List<MetricInfo>> getInfluxDBMetrics(@RequestBody MetricsInfo metricsInfo);

    @GetMapping(Constants.PREFIX_V1 + Constants.OPENSEARCH_PATH)
    ResBody<List<OpenSearchInfo>> getOpenSearchList();
    @PostMapping(Constants.PREFIX_V1 + Constants.OPENSEARCH_PATH + "/logs/vm")
    ResBody<List<Map<String, Object>>> getVMOpenSearchLogs(@RequestBody LogsInfo logsInfo);
    @PostMapping(Constants.PREFIX_V1 + Constants.OPENSEARCH_PATH + "/logs/mcmp")
    ResBody<List<Map<String, Object>>> getMCMPOpenSearchLogs(@RequestBody LogsInfo logsInfo);

    @GetMapping(Constants.PREFIX_V1 + Constants.MININGDB_PATH)
    ResBody<MiningDBInfo> getMiningDB();
    @PutMapping(Constants.PREFIX_V1 + Constants.MININGDB_PATH)
    ResBody<Void> updateMiningDB(@RequestBody Object info);
    @GetMapping(Constants.PREFIX_V1 + Constants.MININGDB_PATH + "/measurement")
    ResBody<List<MeasurementFieldInfo>> getMiningDBFields();
    @GetMapping(Constants.PREFIX_V1 + Constants.MININGDB_PATH + "/tag")
    ResBody<List<MeasurementTagInfo>> getMiningDBTags();
    @PostMapping(Constants.PREFIX_V1 + Constants.MININGDB_PATH + "/metric")
    ResBody<List<MetricInfo>> getMiningDBMetrics(@RequestBody Object metricsInfo);
}
