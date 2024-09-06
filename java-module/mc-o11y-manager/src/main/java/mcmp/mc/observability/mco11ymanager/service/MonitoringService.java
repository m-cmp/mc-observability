package mcmp.mc.observability.mco11ymanager.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11ymanager.client.TumblebugClient;
import mcmp.mc.observability.mco11ymanager.enums.OS;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugSshKey;
import mcmp.mc.observability.mco11ymanager.util.Utils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final TumblebugClient tumblebugClient;
    public String installAgent(String nsId, String targetId) {
        TumblebugMCI mciList = tumblebugClient.getMCIList(nsId);

        if( mciList == null || mciList.getMci() == null || mciList.getMci().isEmpty()) return null;

        try {
            String myIp = switch (OS.parseProperty()) {
                case WINDOWS -> Utils.runExec(new String[]{"powershell", "/c", "$(curl ifconfig.me).Content"});
                case LINUX, UNIX -> Utils.runExec(new String[]{"/bin/sh", "-c", "curl ifconfig.me"});
                default -> throw new IllegalStateException("Unexpected value: " + OS.parseProperty());
            };

            for (TumblebugMCI.MCI mci : mciList.getMci()) {
                if (mci.getVm() == null || mci.getVm().isEmpty()) return null;

                for (TumblebugMCI.Vm vm : mci.getVm()) {
                    if (!vm.getId().equals(targetId)) continue;

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

                        return mci.getId();
                    } catch (JSchException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private String sendCommand(Session session, String command) {
        Channel channel = null;
        String result = null;

        try {
            channel = session.openChannel("exec");
            ChannelExec channelExec = (ChannelExec) channel;
            channelExec.setPty(true);
            channelExec.setCommand(command);

            InputStream is = channel.getInputStream();

            channel.connect();

            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            result = stringBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (channel != null) {
                log.info("result : {}, exit status : {}, command : {}", result, channel.getExitStatus(), command);
                channel.disconnect();
            }
        }

        return result;
    }

}
