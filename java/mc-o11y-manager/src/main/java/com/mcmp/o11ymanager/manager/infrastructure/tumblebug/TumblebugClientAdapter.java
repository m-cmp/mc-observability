package com.mcmp.o11ymanager.manager.infrastructure.tumblebug;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
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
    public TumblebugInfra.Node getNode(String nsId, String infraId, String nodeId) {
        return tumblebugClient.getNode(nsId, infraId, nodeId);
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
    public TumblebugInfra getInfra(String nsId, String infraId) {
        return tumblebugClient.getInfra(nsId, infraId);
    }

    @Override
    public Object sendCommand(String nsId, String infraId, String nodeId, TumblebugCmd command) {
        return tumblebugClient.sendCommand(nsId, infraId, nodeId, command);
    }
}
