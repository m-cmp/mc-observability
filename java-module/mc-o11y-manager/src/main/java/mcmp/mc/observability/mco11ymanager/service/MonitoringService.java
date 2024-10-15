package mcmp.mc.observability.mco11ymanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11ymanager.client.MonitoringClient;
import mcmp.mc.observability.mco11ymanager.client.TumblebugClient;
import mcmp.mc.observability.mco11ymanager.enums.OS;
import mcmp.mc.observability.mco11ymanager.model.PluginDefInfo;
import mcmp.mc.observability.mco11ymanager.model.TumblebugCmd;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugNS;
import mcmp.mc.observability.mco11ymanager.model.dto.MonitoringConfigInfoCreateDTO;
import mcmp.mc.observability.mco11ymanager.util.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final MonitoringClient monitoringClient;
    private final TumblebugClient tumblebugClient;

    public TumblebugNS getNs() {
        return tumblebugClient.getNSList();
    }

    private void configureInputs(String nsId, String mciId, String targetId) {
        MonitoringConfigInfoCreateDTO itemCreateInfo = new MonitoringConfigInfoCreateDTO();

        List<PluginDefInfo> pluginList = monitoringClient.getPluginList().getData();
        for (PluginDefInfo plugin : pluginList) {
            if (plugin == null)
                continue;
            if (plugin.getPluginType().equals("INPUT")) {
                if (plugin.getName().equals("cpu") ||
                        plugin.getName().equals("disk") ||
                        plugin.getName().equals("diskio") ||
                        plugin.getName().equals("mem") ||
                        plugin.getName().equals("processes") ||
                        plugin.getName().equals("swap") ||
                        plugin.getName().equals("system")){
                    itemCreateInfo.setPluginConfig("");
                } else if (plugin.getName().equals("tail")) {
                    itemCreateInfo.setPluginConfig("  files = [\"/var/log/syslog\"]\n" +
                            "  from_beginning = false\n" +
                            "  watch_method = \"inotify\"\n" +
                            "\n" +
                            "  # Data format to parse syslog entries\n" +
                            "  data_format = \"grok\"\n" +
                            "  grok_patterns = [\"%{SYSLOGTIMESTAMP:timestamp} %{SYSLOGHOST:hostname} %{PROG:program}: %{GREEDYDATA:message}\"]\n" +
                            "\n" +
                            "  # Add these fields if you want to tag the logs\n" +
                            "  [inputs.tail.tags]\n" +
                            "    mci_id = \"" + mciId + "\"\n" +
                            "    ns_id = \"" + nsId + "\"\n" +
                            "    target_id = \"" + targetId + "\"");
                } else {
                    continue;
                }

                itemCreateInfo.setPluginSeq(plugin.getSeq());
                itemCreateInfo.setName(plugin.getName());
                monitoringClient.insertItem(nsId, mciId, targetId, itemCreateInfo);
            }
        }
    }

    private void configureOutputs(String nsId, String mciId, String targetId, String myIp) {
        MonitoringConfigInfoCreateDTO storageCreateInfo = new MonitoringConfigInfoCreateDTO();

        List<PluginDefInfo> pluginList = monitoringClient.getPluginList().getData();
        for (PluginDefInfo plugin : pluginList) {
            if (plugin == null)
                continue;
            if (plugin.getPluginType().equals("OUTPUT")) {
                if (plugin.getName().equals("opensearch")){
                    storageCreateInfo.setPluginConfig("  urls = [\"http://"+ myIp + ":9200\"]\n" +
                            "  index_name = \"mc-o11y\"\n" +
                            "  template_name = \"mc-o11y\"\n" +
                            "  namepass = [\"tail\"]");
                } else if (plugin.getName().equals("influxdb")) {
                    storageCreateInfo.setPluginConfig("  urls = [\"http://" + myIp + ":8086\"]\n" +
                            "  database = \"mc-observability\"\n" +
                            "  retention_policy = \"autogen\"\n" +
                            "  username = \"mc-agent\"\n" +
                            "  password = \"mc-agent\"");
                } else {
                    continue;
                }

                storageCreateInfo.setPluginSeq(plugin.getSeq());
                storageCreateInfo.setName(plugin.getName());
                monitoringClient.insertStorage(nsId, mciId, targetId, storageCreateInfo);
            }
        }
    }

    public void configureDefaultConfigs(String nsId, String mciId, String targetId, String myIp) {
        configureInputs(nsId, mciId, targetId);
        configureOutputs(nsId, mciId, targetId, myIp);
    }

    public boolean installAgent(String nsId, String mciId, String targetId) {
        TumblebugMCI mci = tumblebugClient.getMCIList(nsId, mciId);

        try {
            String myIp = switch (OS.parseProperty()) {
                case WINDOWS -> Utils.runExec(new String[]{"powershell", "/c", "$(curl ifconfig.me).Content"}).trim();
                case LINUX, UNIX -> Utils.runExec(new String[]{"/bin/sh", "-c", "curl ifconfig.me"}).trim();
                default -> throw new IllegalStateException("Unexpected value: " + OS.parseProperty());
            };

                for (TumblebugMCI.Vm vm : mci.getVm()) {
                    if (!vm.getId().equals(targetId)) continue;

                    List<String> cmdList = new ArrayList<>();
                    cmdList.add("wget https://github.com/m-cmp/mc-observability/raw/main/java-module/scripts/init.sh -O init.sh");
                    cmdList.add("chmod +x init.sh");
                    cmdList.add("./init.sh " + myIp + " " + nsId + " " + mci.getId() + " " + targetId);
                    TumblebugCmd tumblebugCmd  = new TumblebugCmd();
                    tumblebugCmd.setCommand(cmdList);
                    tumblebugCmd.setUserName(vm.getVmUserName());

                    Object result = tumblebugClient.sendCommand(nsId, mciId, vm.getSubGroupId(), targetId, tumblebugCmd);
                    ObjectMapper objectMapper = new ObjectMapper();
                    System.out.println(objectMapper.writeValueAsString(result));

                    configureDefaultConfigs(nsId, mciId, targetId, myIp);

                    return true;
                }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
