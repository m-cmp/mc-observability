package mcmp.mc.observability.mco11ymanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11ymanager.client.TumblebugClient;
import mcmp.mc.observability.mco11ymanager.enums.OS;
import mcmp.mc.observability.mco11ymanager.model.TargetInfo;
import mcmp.mc.observability.mco11ymanager.model.TumblebugCmd;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugNS;
import mcmp.mc.observability.mco11ymanager.util.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final TumblebugClient tumblebugClient;

    public TumblebugNS getNs() {
        return tumblebugClient.getNSList();
    }

    public String installAgent(String nsId, String mciId, String targetId) {
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
                    cmdList.add("wget https://github.com/m-cmp/mc-observability/raw/main/java-module/scripts/init.sh");
                    cmdList.add("chmod +x init.sh");
                    cmdList.add("./init.sh " + myIp + " " + nsId + " " + mci.getId() + " " + targetId);
                    TumblebugCmd tumblebugCmd  = new TumblebugCmd();
                    tumblebugCmd.setCommand(cmdList);
                    tumblebugCmd.setUserName(vm.getVmUserName());

                    Object result = tumblebugClient.sendCommand(nsId, mciId, vm.getSubGroupId(), targetId, tumblebugCmd);
                    ObjectMapper objectMapper = new ObjectMapper();
                    System.out.println(objectMapper.writeValueAsString(result));
                }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}
