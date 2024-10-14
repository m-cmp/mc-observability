package mcmp.mc.observability.mco11ymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.model.*;
import mcmp.mc.observability.mco11ymanager.client.MonitoringClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import mcmp.mc.observability.mco11ymanager.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.mco11ymanager.service.MonitoringService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1)
public class MonitoringController {

    private final MonitoringClient monitoringClient;
    private final MonitoringService monitoringService;

    // monitoring test api
    @GetMapping("/monitoring/ns")
    @Operation(operationId = "GetNSs", summary = "Get all mc-observability management namespace list",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public TumblebugNS getNS() {
        return monitoringService.getNs();
    }

    // monitoring plugins api
    @GetMapping("/monitoring/plugins")
    @Operation(operationId = "GetPlugins", summary = "Get all available monitoring plugin list",
            tags = "[System] environment")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<PluginDefInfo>> getPluginList() {
        return monitoringClient.getPluginList();
    }

    // monitoring target api
    @GetMapping("/monitoring/target")
    @Operation(operationId = "GetTargets", summary = "Get all mc-observability management target list",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<TargetInfo>> getTargetList() {
        return monitoringClient.getTargetList();
    }

    @GetMapping("/monitoring/{nsId}/{mciId}/target")
    @Operation(operationId = "GetTargetsNSMCI", summary = "Get all mc-observability management target list in specified NS and MCI",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<TargetInfo>> getTargetListNSMCI(@PathVariable("nsId") String nsId, @PathVariable("mciId") String mciId) {
        return monitoringClient.getTargetListNSMCI(nsId, mciId);
    }

    @GetMapping(Constants.TARGET_PATH)
    @Operation(operationId = "GetTarget", summary = "Get monitoring target",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<TargetInfo> getTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getTarget(nsId, mciId, targetId);
    }

    @PostMapping(Constants.TARGET_PATH)
    @Operation(operationId = "PostTarget", summary = "Add new monitoring target (MC-O11y-Agent Install)",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody insertTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        ResBody<TargetInfo> obj = monitoringClient.getTarget(nsId, mciId, targetId);
        if( obj.getData() != null ) return null;

        monitoringService.installAgent(nsId, mciId, targetId, targetInfo);

        if( mciId == null ) return null;

        return monitoringClient.insertTarget(nsId, mciId, targetId, targetInfo);
    }

    @PutMapping(Constants.TARGET_PATH)
    @Operation(operationId = "PutTarget", summary = "Update target information",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody updateTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        return monitoringClient.updateTarget(nsId, mciId, targetId, targetInfo);
    }

    @DeleteMapping(Constants.TARGET_PATH)
    @Operation(operationId = "DeleteTarget", summary = "Delete monitoring agent & management target",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody deleteTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.deleteTarget(nsId, mciId, targetId);
    }

    // cb-spider monitoring api
    @GetMapping(Constants.TARGET_CSP_PATH)
    @Operation(operationId = "GetCSP", summary = "Get target monitoring data from the CSP",
            tags = "[Monitoring CSP] Monitoring target from the CSP")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody<SpiderMonitoringInfo.Data> getCSP(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable String measurement) {
        return monitoringClient.getCSP(nsId, mciId, targetId, measurement);
    }

    // monitoring item api
    @GetMapping(Constants.TARGET_ITEM_PATH)
    @Operation(operationId = "GetItems", summary = "Get all target monitoring item list",
            tags = "[Monitoring item] Monitoring target item management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody<List<MonitoringConfigInfo>> getItemList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getItemList(nsId, mciId, targetId);
    }

    @PostMapping(Constants.TARGET_ITEM_PATH)
    @Operation(operationId = "PostItem", summary = "Add target monitoring item",
            tags = "[Monitoring item] Monitoring target item management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody insertItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringClient.insertItem(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @PutMapping(Constants.TARGET_ITEM_PATH)
    @Operation(operationId = "PutItem", summary = "Update target monitoring item",
            tags = "[Monitoring item] Monitoring target item management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody updateItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringClient.updateItem(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @DeleteMapping(Constants.TARGET_ITEM_PATH + "/{itemSeq}")
    @Operation(operationId = "DeleteItem", summary = "Delete target monitoring item",
            tags = "[Monitoring item] Monitoring target item management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody deleteItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long itemSeq) {
        return monitoringClient.deleteItem(nsId, mciId, targetId, itemSeq);
    }

    // monitoring storage api
    @GetMapping(Constants.TARGET_STORAGE_PATH)
    @Operation(operationId = "GetStorages", summary = "Get all target monitoring storage list",
            tags = "[Monitoring storage] Monitoring target storage management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody<List<MonitoringConfigInfo>> getStorageList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getStorageList(nsId, mciId, targetId);
    }

    @PostMapping(Constants.TARGET_STORAGE_PATH)
    @Operation(operationId = "PostStorage", summary = "Add target monitoring storage",
            tags = "[Monitoring storage] Monitoring target storage management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody insertStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringClient.insertStorage(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @PutMapping(Constants.TARGET_STORAGE_PATH)
    @Operation(operationId = "PutStorage", summary = "Update target monitoring storage",
            tags = "[Monitoring storage] Monitoring target storage management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody updateStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringClient.updateStorage(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @DeleteMapping(Constants.TARGET_STORAGE_PATH + "/{storageSeq}")
    @Operation(operationId = "DeleteStorage", summary = "Delete target monitoring storage",
            tags = "[Monitoring storage] Monitoring target storage management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    ResBody deleteStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long storageSeq) {
        return monitoringClient.deleteStorage(nsId, mciId, targetId, storageSeq);
    }

    // monitoring influxdb metric api
    @GetMapping(Constants.INFLUXDB_PATH)
    @Operation(operationId = "GetInfluxDBs", summary = "Get all InfluxDB list",
            tags = "[Monitoring metric] Monitoring metric")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<InfluxDBInfo>> getInfluxDBList() {
        return monitoringClient.getInfluxDBList();
    }

    @GetMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/measurement")
    @Operation(operationId = "GetInfluxDBMeasurements", summary = "Get collected measurement & field list",
            tags = "[Monitoring metric] Monitoring metric")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MeasurementFieldInfo>> getInfluxDBFields(@PathVariable Long influxDBSeq) {
        return monitoringClient.getInfluxDBFields(influxDBSeq);
    }

    @GetMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/tag")
    @Operation(operationId = "GetInfluxDBTags", summary = "Get collected measurement tag list",
            tags = "[Monitoring metric] Monitoring metric")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MeasurementTagInfo>> getInfluxDBTags(@PathVariable Long influxDBSeq) {
        return monitoringClient.getInfluxDBTags(influxDBSeq);
    }

    @PostMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/metric")
    @Operation(operationId = "GetInfluxDBMetrics", summary = "Get collected metric",
            tags = "[Monitoring metric] Monitoring metric")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MetricInfo>> getInfluxDBMetrics(@PathVariable Long influxDBSeq, @RequestBody MetricsInfo metricsInfo) {
        return monitoringClient.getInfluxDBMetrics(influxDBSeq, metricsInfo);
    }

    // monitoring opensearch log api
    @GetMapping(Constants.OPENSEARCH_PATH)
    @Operation(operationId = "GetOpenSearches", summary = "Get all OpenSearch list",
            tags = "[Monitoring log] Monitoring log")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<OpenSearchInfo>> getOpenSearchList() {
        return monitoringClient.getOpenSearchList();
    }

    @PostMapping(Constants.OPENSEARCH_PATH + "/logs")
    @Operation(operationId = "GetOpenSearchLogs", summary = "Get collected logs from OpenSearch",
            tags = "[Monitoring log] Monitoring log")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<Map<String, Object>>> getOpenSearchLogs(@RequestBody LogsInfo logsInfo) {
        return monitoringClient.getOpenSearchLogs(logsInfo);
    }

    // monitoring miningdb api
    @GetMapping(Constants.MININGDB_PATH)
    @Operation(operationId = "GetMiningDBs", summary = "Get mining influxdb detail",
            tags = "[Target] Monitoring target management")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<MiningDBInfo> getMiningDB() {
        return monitoringClient.getMiningDB();
    }

    @PutMapping(Constants.MININGDB_PATH)
    @Operation(operationId = "PutMiningDB", summary = "Update mining influxdb info",
            tags = "[Mining InfluxDB] Monitoring metric downsampling")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<Void> updateMiningDB(@RequestBody MiningDBSetDTO info) {
        return monitoringClient.updateMiningDB(info);
    }

    @GetMapping(Constants.MININGDB_PATH + "/measurement")
    @Operation(operationId = "GetMiningDBMeasurements",summary = "Get downsampling measurement & field list",
            tags = "[Mining InfluxDB] Monitoring metric downsampling")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MeasurementFieldInfo>> getMiningDBFields() {
        return monitoringClient.getMiningDBFields();
    }

    @GetMapping(Constants.MININGDB_PATH + "/tag")
    @Operation(operationId = "GetMiningDBTags\"", summary = "Get downsampling metric tag list",
            tags = "[Mining InfluxDB] Monitoring metric downsampling")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MeasurementTagInfo>> getMiningDBTags() {
        return monitoringClient.getMiningDBTags();
    }

    @PostMapping(Constants.MININGDB_PATH + "/metric")
    @Operation(operationId = "GetMiningDBMetrics", summary = "Get downsampling metric",
            tags = "[Mining InfluxDB] Monitoring metric downsampling")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
    public ResBody<List<MetricInfo>> getMiningDBMetrics(@RequestBody MetricsInfo metricsInfo) {
        return monitoringClient.getMiningDBMetrics(metricsInfo);
    }
}
