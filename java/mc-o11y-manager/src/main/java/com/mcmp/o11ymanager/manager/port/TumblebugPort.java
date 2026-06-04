package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;

public interface TumblebugPort {

    TumblebugInfra.Node getNode(String nsId, String infraId, String nodeId);

    TumblebugSshKey getSshKey(String nsId, String sshKeyId);

    TumblebugNS getNSList();

    TumblebugInfra getInfra(String nsId, String infraId);

    Object sendCommand(String nsId, String infraId, String nodeId, TumblebugCmd command);
}
