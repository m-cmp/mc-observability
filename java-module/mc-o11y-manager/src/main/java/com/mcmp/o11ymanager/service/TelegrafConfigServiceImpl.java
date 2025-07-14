package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.host.ConfigResponseDTO;
import com.mcmp.o11ymanager.global.annotation.Base64Encode;
import com.mcmp.o11ymanager.global.definition.ConfigDefinition;
import com.mcmp.o11ymanager.service.interfaces.FileService;
import com.mcmp.o11ymanager.service.interfaces.TelegrafConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegrafConfigServiceImpl implements TelegrafConfigService {

//    private final FileService fileService;
//
//    @Value("${influxdb.url}")
//    private String influxDBURL;
//
//    @Value("${influxdb.database}")
//    private String influxDBDatabase;
//
//    @Value("${influxdb.username}")
//    private String influxDBUsername;
//
//    @Value("${influxdb.password}")
//    private String influxDBPassword;
//
//    private final ClassPathResource telegrafConfigTemplate = new ClassPathResource("telegraf_template.conf");
//
//    private final ClassPathResource telegrafConfigGlobal = new ClassPathResource("telegraf_global");
//    private final ClassPathResource telegrafConfigAgent = new ClassPathResource("telegraf_agent");
//    private final ClassPathResource telegrafConfigInputsCPU = new ClassPathResource(
//            "telegraf_inputs_cpu");
//    private final ClassPathResource telegrafConfigInputsDisk = new ClassPathResource(
//            "telegraf_inputs_disk");
//    private final ClassPathResource telegrafConfigInputsDiskIO = new ClassPathResource(
//            "telegraf_inputs_diskio");
//    private final ClassPathResource telegrafConfigInputsMem = new ClassPathResource(
//            "telegraf_inputs_mem");
//    private final ClassPathResource telegrafConfigInputsNet = new ClassPathResource(
//            "telegraf_inputs_net");
//    private final ClassPathResource telegrafConfigInputsProcesses = new ClassPathResource(
//            "telegraf_inputs_processes");
//    private final ClassPathResource telegrafConfigInputsProcstat = new ClassPathResource(
//            "telegraf_inputs_procstat");
//    private final ClassPathResource telegrafConfigInputsSwap = new ClassPathResource(
//            "telegraf_inputs_swap");
//    private final ClassPathResource telegrafConfigInputsSystem = new ClassPathResource(
//            "telegraf_inputs_system");
//    private final ClassPathResource telegrafConfigInputsNVIDIASMI = new ClassPathResource(
//            "telegraf_inputs_nvidia_smi");
//
//    private final ClassPathResource telegrafConfigOutputsInfluxDB = new ClassPathResource(
//            "telegraf_outputs_influxdb");
//
//    public static final String CONFIG_METRIC_CPU = "cpu";
//    public static final String CONFIG_METRIC_DISK = "disk";
//    public static final String CONFIG_METRIC_DISKIO = "diskio";
//    public static final String CONFIG_METRIC_MEM = "mem";
//    public static final String CONFIG_METRIC_NET = "net";
//    public static final String CONFIG_METRIC_PROCESSES = "processes";
//    public static final String CONFIG_METRIC_PROCSTAT = "procstat";
//    public static final String CONFIG_METRIC_SWAP = "swap";
//    public static final String CONFIG_METRIC_SYSTEM = "system";
//    public static final String CONFIG_METRIC_GPU = "gpu";
//
//    public static final String CONFIG_DEFAULT_METRICS =
//            CONFIG_METRIC_CPU + "," +
//                    CONFIG_METRIC_DISK + "," +
//                    CONFIG_METRIC_DISKIO + "," +
//                    CONFIG_METRIC_MEM + "," +
//                    CONFIG_METRIC_NET + "," +
//                    CONFIG_METRIC_PROCESSES + "," +
//                    CONFIG_METRIC_PROCSTAT + "," +
//                    CONFIG_METRIC_SWAP + "," +
//                    CONFIG_METRIC_SYSTEM;
//
//

//    @Override
//    @Base64Encode
//    public ConfigResponseDTO getTelegrafConfigTemplate(String path) {
//        if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG)) {
//            return ConfigResponseDTO.builder().content(fileService.getClassResourceContent(telegrafConfigTemplate)).build();
//        }
//
//        throw new IllegalArgumentException("Invalid Telegraf template path");
//    }

//    @Override
//    public String generateTelegrafConfig(String uuid, String hostType, String metrics) {
//        String errMsg;
//
//
//        if (!telegrafConfigGlobal.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigGlobal";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigAgent.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigAgent";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsCPU.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsCPU";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsDisk.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsDisk";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsDiskIO.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsDiskIO";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsMem.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsMem";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsNet.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsNet";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsProcesses.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsProcesses";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsProcstat.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsProcstat";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsSwap.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsSwap";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsSystem.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsSystem";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigInputsNVIDIASMI.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigInputsNVIDIASMI";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        if (!telegrafConfigOutputsInfluxDB.exists()) {
//            errMsg = "Invalid filePath : telegrafConfigOutputsInfluxDB";
//            log.error(errMsg);
//            throw new RuntimeException(errMsg);
//        }
//
//        StringBuilder sb = new StringBuilder();
//
//        fileService.appendConfig(telegrafConfigGlobal, sb);
//        fileService.appendConfig(telegrafConfigAgent, sb);
//
//        String[] metricsSplit = Arrays.stream(metrics.replace(" ", "").split(","))
//                .distinct()
//                .toArray(String[]::new);
//
//        for (String metric : metricsSplit) {
//            switch (metric) {
//                case CONFIG_METRIC_CPU:
//                    fileService.appendConfig(telegrafConfigInputsCPU, sb);
//                    break;
//                case CONFIG_METRIC_DISK:
//                    fileService.appendConfig(telegrafConfigInputsDisk, sb);
//                    break;
//                case CONFIG_METRIC_DISKIO:
//                    fileService.appendConfig(telegrafConfigInputsDiskIO, sb);
//                    break;
//                case CONFIG_METRIC_MEM:
//                    fileService.appendConfig(telegrafConfigInputsMem, sb);
//                    break;
//                case CONFIG_METRIC_NET:
//                    fileService.appendConfig(telegrafConfigInputsNet, sb);
//                    break;
//                case CONFIG_METRIC_PROCESSES:
//                    fileService.appendConfig(telegrafConfigInputsProcesses, sb);
//                    break;
//                case CONFIG_METRIC_PROCSTAT:
//                    fileService.appendConfig(telegrafConfigInputsProcstat, sb);
//                    break;
//                case CONFIG_METRIC_SWAP:
//                    fileService.appendConfig(telegrafConfigInputsSwap, sb);
//                    break;
//                case CONFIG_METRIC_SYSTEM:
//                    fileService.appendConfig(telegrafConfigInputsSystem, sb);
//                    break;
//                case CONFIG_METRIC_GPU:
//                    fileService.appendConfig(telegrafConfigInputsNVIDIASMI, sb);
//                    break;
//                default:
//                    throw new RuntimeException("Invalid metric: " + metric);
//            }
//        }
//
//        fileService.appendConfig(telegrafConfigOutputsInfluxDB, sb);
//
//        return sb.toString()
//                .replace("@ID", uuid)
//                .replace("@TYPE", hostType)
//                .replace("@URL", influxDBURL)
//                .replace("@DATABASE", influxDBDatabase)
//                .replace("@USERNAME", influxDBUsername)
//                .replace("@PASSWORD", influxDBPassword);
//    }

}
