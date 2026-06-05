package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegrafConfigFacadeService {

    private final FileService fileService;

    private final InfluxDbFacadeService influxDbFacadeService;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    private final ClassPathResource telegrafConfigGlobal = new ClassPathResource("telegraf_global");
    private final ClassPathResource telegrafConfigAgent = new ClassPathResource("telegraf_agent");
    private final ClassPathResource telegrafProcessorsRegex =
            new ClassPathResource("telegraf_processors_regex");
    private final ClassPathResource telegrafConfigInputsCPU =
            new ClassPathResource("telegraf_inputs_cpu");
    private final ClassPathResource telegrafConfigInputsDisk =
            new ClassPathResource("telegraf_inputs_disk");
    private final ClassPathResource telegrafConfigInputsDiskIO =
            new ClassPathResource("telegraf_inputs_diskio");
    private final ClassPathResource telegrafConfigInputsMem =
            new ClassPathResource("telegraf_inputs_mem");
    private final ClassPathResource telegrafConfigInputsNet =
            new ClassPathResource("telegraf_inputs_net");
    private final ClassPathResource telegrafConfigInputsProcesses =
            new ClassPathResource("telegraf_inputs_processes");
    private final ClassPathResource telegrafConfigInputsProcstat =
            new ClassPathResource("telegraf_inputs_procstat");
    private final ClassPathResource telegrafConfigInputsSwap =
            new ClassPathResource("telegraf_inputs_swap");
    private final ClassPathResource telegrafConfigInputsSystem =
            new ClassPathResource("telegraf_inputs_system");
    private final ClassPathResource telegrafConfigOutputsInfluxDB =
            new ClassPathResource("telegraf_outputs_influxdb");
    // GPU(DCGM Exporter) 메트릭 수집용: prometheus input + starlark processor 쌍으로 동작.
    // starlark가 DCGM_FI_*/DCGM_EXP_* 메트릭명을 `dcgm` measurement의 필드로 변환한다.
    private final ClassPathResource telegrafConfigInputsPrometheus =
            new ClassPathResource("telegraf_inputs_prometheus");
    private final ClassPathResource telegrafProcessorsStarlark =
            new ClassPathResource("telegraf_processors_starlark");

    @Value("${dcgm-exporter.url:http://localhost:9400/metrics}")
    private String dcgmExporterUrl;

    public static final String CONFIG_METRIC_CPU = "cpu";
    public static final String CONFIG_METRIC_DISK = "disk";
    public static final String CONFIG_METRIC_DISKIO = "diskio";
    public static final String CONFIG_METRIC_MEM = "mem";
    public static final String CONFIG_METRIC_NET = "net";
    public static final String CONFIG_METRIC_PROCESSES = "processes";
    public static final String CONFIG_METRIC_PROCSTAT = "procstat";
    public static final String CONFIG_METRIC_SWAP = "swap";
    public static final String CONFIG_METRIC_SYSTEM = "system";
    public static final String CONFIG_METRIC_GPU = "gpu";

    public static final String CONFIG_DEFAULT_METRICS =
            CONFIG_METRIC_CPU
                    + ","
                    + CONFIG_METRIC_DISK
                    + ","
                    + CONFIG_METRIC_DISKIO
                    + ","
                    + CONFIG_METRIC_MEM
                    + ","
                    + CONFIG_METRIC_NET
                    + ","
                    + CONFIG_METRIC_PROCESSES
                    + ","
                    + CONFIG_METRIC_PROCSTAT
                    + ","
                    + CONFIG_METRIC_SWAP
                    + ","
                    + CONFIG_METRIC_SYSTEM;

    public String initTelegrafConfig(String nsId, String mciId, String vmId) {
        return generateTelegrafConfig(nsId, mciId, vmId, CONFIG_DEFAULT_METRICS);
    }

    public String initTelegrafConfig(String nsId, String mciId, String vmId, boolean gpu) {
        String metrics =
                gpu ? CONFIG_DEFAULT_METRICS + "," + CONFIG_METRIC_GPU : CONFIG_DEFAULT_METRICS;
        return generateTelegrafConfig(nsId, mciId, vmId, metrics);
    }

    public String generateTelegrafConfig(String nsId, String mciId, String vmId, String metrics) {
        String errMsg;

        if (!telegrafConfigGlobal.exists()) {
            errMsg = "Invalid filePath : telegrafConfigGlobal";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigAgent.exists()) {
            errMsg = "Invalid filePath : telegrafConfigAgent";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafProcessorsRegex.exists()) {
            errMsg = "Invalid filePath : telegrafProcessorsRegex";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsCPU.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsCPU";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsDisk.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsDisk";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsDiskIO.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsDiskIO";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsMem.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsMem";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsNet.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsNet";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsProcesses.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsProcesses";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsProcstat.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsProcstat";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsSwap.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsSwap";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsSystem.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsSystem";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigOutputsInfluxDB.exists()) {
            errMsg = "Invalid filePath : telegrafConfigOutputsInfluxDB";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsPrometheus.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsPrometheus";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafProcessorsStarlark.exists()) {
            errMsg = "Invalid filePath : telegrafProcessorsStarlark";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        StringBuilder sb = new StringBuilder();

        fileService.appendConfig(telegrafConfigGlobal, sb);
        fileService.appendConfig(telegrafConfigAgent, sb);

        String[] metricsSplit =
                Arrays.stream(metrics.replace(" ", "").split(","))
                        .distinct()
                        .toArray(String[]::new);

        fileService.appendConfig(telegrafProcessorsRegex, sb);

        for (String metric : metricsSplit) {
            switch (metric) {
                case CONFIG_METRIC_CPU:
                    fileService.appendConfig(telegrafConfigInputsCPU, sb);
                    break;
                case CONFIG_METRIC_DISK:
                    fileService.appendConfig(telegrafConfigInputsDisk, sb);
                    break;
                case CONFIG_METRIC_DISKIO:
                    fileService.appendConfig(telegrafConfigInputsDiskIO, sb);
                    break;
                case CONFIG_METRIC_MEM:
                    fileService.appendConfig(telegrafConfigInputsMem, sb);
                    break;
                case CONFIG_METRIC_NET:
                    fileService.appendConfig(telegrafConfigInputsNet, sb);
                    break;
                case CONFIG_METRIC_PROCESSES:
                    fileService.appendConfig(telegrafConfigInputsProcesses, sb);
                    break;
                case CONFIG_METRIC_PROCSTAT:
                    fileService.appendConfig(telegrafConfigInputsProcstat, sb);
                    break;
                case CONFIG_METRIC_SWAP:
                    fileService.appendConfig(telegrafConfigInputsSwap, sb);
                    break;
                case CONFIG_METRIC_SYSTEM:
                    fileService.appendConfig(telegrafConfigInputsSystem, sb);
                    break;
                case CONFIG_METRIC_GPU:
                    // DCGM Exporter(:9400/metrics) 스크랩 + DCGM_FI_* -> dcgm measurement 변환
                    fileService.appendConfig(telegrafConfigInputsPrometheus, sb);
                    fileService.appendConfig(telegrafProcessorsStarlark, sb);
                    break;
                default:
                    throw new RuntimeException("Invalid metric: " + metric);
            }
        }

        fileService.appendConfig(telegrafConfigOutputsInfluxDB, sb);

        var out = influxDbFacadeService.resolveForVM(nsId, mciId);

        String finalNsId = (nsId != null) ? nsId : "";
        log.debug(finalNsId);

        String finalMciId = (mciId != null) ? mciId : "";
        log.debug(finalMciId);

        String finalVmId = (vmId != null) ? vmId : "";
        log.debug(finalVmId);

        return sb.toString()
                .replace("@DCGM_EXPORTER_URL", dcgmExporterUrl)
                .replace("@SITE_CODE", deploySiteCode)
                .replace("@NS_ID", finalNsId)
                .replace("@INFRA_ID", finalMciId)
                .replace("@NODE_ID", finalVmId)
                .replace("@URL", out.getUrl())
                .replace("@DATABASE", out.getDatabase())
                .replace("@USERNAME", out.getUsername())
                .replace("@PASSWORD", out.getPassword());
    }
}
