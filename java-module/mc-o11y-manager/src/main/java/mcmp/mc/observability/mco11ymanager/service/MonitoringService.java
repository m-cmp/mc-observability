package mcmp.mc.observability.mco11ymanager.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11ymanager.client.TumblebugClient;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugSshKey;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final TumblebugClient tumblebugClient;
    public boolean installAgent(String nsId, String targetId) {
        TumblebugMCI mciList = tumblebugClient.getMCIList(nsId);

        if( mciList == null || mciList.getMci() == null || mciList.getMci().isEmpty()) return false;

        for( TumblebugMCI.MCI mci : mciList.getMci() ) {
            if (mci.getVm() == null || mci.getVm().isEmpty()) return false;

            for (TumblebugMCI.Vm vm : mci.getVm()) {
                if( !vm.getId().equals(targetId) ) continue;

                TumblebugSshKey tumblebugSshKey = tumblebugClient.getSshKey(nsId, vm.getSshKeyId());

                JSch jSch = new JSch();
                try {
                    jSch.addIdentity(tumblebugSshKey.getSshKey().get(0).getPrivateKey());
                    Session session = jSch.getSession(vm.getVmUserAccount(), vm.getPublicIP(), Integer.parseInt(vm.getSshPort()));
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();

                    // install command

                    session.disconnect();

                    return true;
                } catch (JSchException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }
}
