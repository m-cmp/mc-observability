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
    // GPU 메트릭 수집용: nvidia_smi input + starlark processor 쌍으로 동작.
    // nvidia-smi는 드라이버에 동봉되므로 노드에 추가로 설치할 것이 없다.
    // starlark가 nvidia_smi 필드를 `dcgm` measurement의 필드명으로 변환하므로
    // 소비자(GpuMetricKeyField, 대시보드, 알람)는 수집기 교체의 영향을 받지 않는다.
    private final ClassPathResource telegrafConfigInputsNvidiaSmi =
            new ClassPathResource("telegraf_inputs_nvidia_smi");
    private final ClassPathResource telegrafProcessorsNvidiaSmiToDcgm =
            new ClassPathResource("telegraf_processors_nvidia_smi_to_dcgm");
    // MIG가 켜진 GPU는 NVML의 장치 단위 utilization 카운터가 꺼져 nvidia-smi로 사용률을 못 얻는다.
    // GPM API가 그 경우의 유일한 출처이고, 아래 processor가 nvidia-smi가 값을 낸 경우엔 버린다.
    private final ClassPathResource telegrafConfigInputsNvidiaGpm =
            new ClassPathResource("telegraf_inputs_nvidia_gpm");
    private final ClassPathResource telegrafProcessorsGpmToDcgm =
            new ClassPathResource("telegraf_processors_gpm_to_dcgm");

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

    public String initTelegrafConfig(String nsId, String infraId, String nodeId) {
        return generateTelegrafConfig(nsId, infraId, nodeId, CONFIG_DEFAULT_METRICS);
    }

    public String initTelegrafConfig(String nsId, String infraId, String nodeId, boolean gpu) {
        String metrics =
                gpu ? CONFIG_DEFAULT_METRICS + "," + CONFIG_METRIC_GPU : CONFIG_DEFAULT_METRICS;
        return generateTelegrafConfig(nsId, infraId, nodeId, metrics);
    }

    public String generateTelegrafConfig(
            String nsId, String infraId, String nodeId, String metrics) {
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

        if (!telegrafConfigInputsNvidiaSmi.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsNvidiaSmi";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafProcessorsNvidiaSmiToDcgm.exists()) {
            errMsg = "Invalid filePath : telegrafProcessorsNvidiaSmiToDcgm";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafConfigInputsNvidiaGpm.exists()) {
            errMsg = "Invalid filePath : telegrafConfigInputsNvidiaGpm";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (!telegrafProcessorsGpmToDcgm.exists()) {
            errMsg = "Invalid filePath : telegrafProcessorsGpmToDcgm";
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
                    // nvidia-smi 직접 수집 + nvidia_smi -> dcgm measurement 변환.
                    // GPM은 MIG 게스트에서만 값이 남는 보완 경로라 그 뒤에 온다
                    // (gpm processor가 dcgm 포인트를 먼저 봐야 시리즈 태그를 배운다).
                    fileService.appendConfig(telegrafConfigInputsNvidiaSmi, sb);
                    fileService.appendConfig(telegrafProcessorsNvidiaSmiToDcgm, sb);
                    fileService.appendConfig(telegrafConfigInputsNvidiaGpm, sb);
                    fileService.appendConfig(telegrafProcessorsGpmToDcgm, sb);
                    break;
                default:
                    throw new RuntimeException("Invalid metric: " + metric);
            }
        }

        fileService.appendConfig(telegrafConfigOutputsInfluxDB, sb);

        var out = influxDbFacadeService.resolveForVM(nsId, infraId);

        String finalNsId = (nsId != null) ? nsId : "";
        log.debug(finalNsId);

        String finalInfraId = (infraId != null) ? infraId : "";
        log.debug(finalInfraId);

        String finalNodeId = (nodeId != null) ? nodeId : "";
        log.debug(finalNodeId);

        return sb.toString()
                .replace("@SITE_CODE", deploySiteCode)
                .replace("@NS_ID", finalNsId)
                .replace("@INFRA_ID", finalInfraId)
                .replace("@NODE_ID", finalNodeId)
                .replace("@URL", out.getUrl())
                .replace("@DATABASE", out.getDatabase())
                .replace("@USERNAME", out.getUsername())
                .replace("@PASSWORD", out.getPassword());
    }
}
