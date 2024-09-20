package mcmp.mc.observability.mco11ymanager.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.client.MonitoringClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import mcmp.mc.observability.mco11ymanager.model.ResBody;
import mcmp.mc.observability.mco11ymanager.model.TargetInfo;
import mcmp.mc.observability.mco11ymanager.service.MonitoringService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1)
public class MonitoringController {

    private final MonitoringClient monitoringClient;
    private final MonitoringService monitoringService;

    // monitoring test api
    @GetMapping("/monitoring/ns")
    public Object getNS() {
        return monitoringService.getNs();
    }
    // monitoring plugins api
    @GetMapping("/monitoring/plugins")
    public Object getPluginList() {
        return monitoringClient.getPluginList();
    }
    // monitoring target api
    @GetMapping("/monitoring/target")
    public Object getTargetList() {
        return monitoringClient.getTargetList();
    }
    @GetMapping("/monitoring/{nsId}/{mciId}/target")
    public Object getTargetListNSMCI(@PathVariable("nsId") String nsId, @PathVariable("mciId") String mciId) {
        return monitoringClient.getTargetListNSMCI(nsId, mciId);
    }
    @GetMapping(Constants.TARGET_PATH)
    public ResBody getTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getTarget(nsId, mciId, targetId);
    }
    @PostMapping(Constants.TARGET_PATH)
    public Object insertTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        ResBody obj = monitoringClient.getTarget(nsId, mciId, targetId);
        if( obj.getData() != null ) return null;

        monitoringService.installAgent(nsId, mciId, targetId, targetInfo);

        if( mciId == null ) return null;

        return monitoringClient.insertTarget(nsId, mciId, targetId, targetInfo);
    }
    @PutMapping(Constants.TARGET_PATH)
    public Object updateTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object targetInfo) {
        return monitoringClient.updateTarget(nsId, mciId, targetId, targetInfo);
    }
    @DeleteMapping(Constants.TARGET_PATH)
    public Object deleteTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.deleteTarget(nsId, mciId, targetId);
    }

    // cb-spider monitoring api
    @GetMapping(Constants.TARGET_CSP_PATH)
    Object getCSP(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable String metricType) {
        return monitoringClient.getCSP(nsId, mciId, targetId, metricType);
    }

    // monitoring item api
    @GetMapping(Constants.TARGET_ITEM_PATH)
    Object getItemList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getItemList(nsId, mciId, targetId);
    }
    @PostMapping(Constants.TARGET_ITEM_PATH)
    Object insertItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object monitoringConfigInfo) {
        return monitoringClient.insertItem(nsId, mciId, targetId, monitoringConfigInfo);
    }
    @PutMapping(Constants.TARGET_ITEM_PATH)
    Object updateItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object monitoringConfigInfo) {
        return monitoringClient.updateItem(nsId, mciId, targetId, monitoringConfigInfo);
    }
    @DeleteMapping(Constants.TARGET_ITEM_PATH + "/{itemSeq}")
    Object deleteItem(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long itemSeq) {
        return monitoringClient.deleteItem(nsId, mciId, targetId, itemSeq);
    }

    // monitoring storage api
    @GetMapping(Constants.TARGET_STORAGE_PATH)
    Object getStorageList(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return monitoringClient.getStorageList(nsId, mciId, targetId);
    }
    @PostMapping(Constants.TARGET_STORAGE_PATH)
    Object insertStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object monitoringConfigInfo) {
        return monitoringClient.insertStorage(nsId, mciId, targetId, monitoringConfigInfo);
    }
    @PutMapping(Constants.TARGET_STORAGE_PATH)
    Object updateStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody Object monitoringConfigInfo) {
        return monitoringClient.updateStorage(nsId, mciId, targetId, monitoringConfigInfo);
    }
    @DeleteMapping(Constants.TARGET_STORAGE_PATH + "/{storageSeq}")
    Object deleteStorage(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long storageSeq) {
        return monitoringClient.deleteStorage(nsId, mciId, targetId, storageSeq);
    }

    // monitoring influxdb metric api
    @GetMapping(Constants.INFLUXDB_PATH)
    public Object getInfluxDBList() {
        return monitoringClient.getInfluxDBList();
    }

    @GetMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/measurement")
    public Object getInfluxDBFields(@PathVariable Long influxDBSeq) {
        return monitoringClient.getInfluxDBFields(influxDBSeq);
    }

    @GetMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/tag")
    public Object getInfluxDBTags(@PathVariable Long influxDBSeq) {
        return monitoringClient.getInfluxDBTags(influxDBSeq);
    }

    @PostMapping(Constants.INFLUXDB_PATH + "/{influxDBSeq}/metric")
    public Object getInfluxDBMetrics(@PathVariable Long influxDBSeq, @RequestBody Object metricsInfo) {
        return monitoringClient.getInfluxDBMetrics(influxDBSeq, metricsInfo);
    }

    // monitoring opensearch log api
    @GetMapping(Constants.OPENSEARCH_PATH)
    public Object getOpensearchList() {
        return monitoringClient.getOpensearchList();
    }

    @PostMapping(Constants.OPENSEARCH_PATH + "/{opensearchSeq}/logs")
    public Object getOpensearchLogs(@PathVariable Long opensearchSeq, @RequestBody Object logsInfo) {
        return monitoringClient.getOpensearchLogs(opensearchSeq, logsInfo);
    }

    // monitoring miningdb api
    @GetMapping(Constants.MININGDB_PATH)
    public Object getMiningDB() {
        return monitoringClient.getMiningDB();
    }

    @PostMapping(Constants.MININGDB_PATH)
    public Object updateMiningDB(@RequestBody Object info) {
        return monitoringClient.updateMiningDB(info);
    }

    @GetMapping(Constants.MININGDB_PATH + "/measurement")
    public Object getMiningDBFields() {
        return monitoringClient.getMiningDBFields();
    }

    @GetMapping(Constants.MININGDB_PATH + "/tag")
    public Object getMiningDBTags() {
        return monitoringClient.getMiningDBTags();
    }

    @PostMapping(Constants.MININGDB_PATH + "/metric")
    public Object getMiningDBMetrics(@RequestBody Object metricsInfo) {
        return monitoringClient.getMiningDBMetrics(metricsInfo);
    }
}
