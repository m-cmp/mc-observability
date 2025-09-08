package com.mcmp.o11ymanager.manager.infrastructure.tumblebug;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TumblebugClientAdapter implements TumblebugPort {

    private final TumblebugClient tumblebugClient;

    @Override
    public TumblebugMCI.Vm getVM(String nsId, String mciId, String vmId) {
        return tumblebugClient.getVM(nsId, mciId, vmId);
    }

    @Override
    public TumblebugSshKey getSshKey(String nsId, String sshKeyId) {
        return tumblebugClient.getSshKey(nsId, sshKeyId);
    }

    @Override
    public TumblebugNS getNSList() {
        return tumblebugClient.getNSList();
    }

    @Override
    public TumblebugMCI getMCIList(String nsId, String mciId) {
        return tumblebugClient.getMCIList(nsId, mciId);
    }

    @Override
    public Object sendCommand(String nsId, String mciId, String vmId, TumblebugCmd command) {
        return tumblebugClient.sendCommand(nsId, mciId, vmId, command);
    }
}
