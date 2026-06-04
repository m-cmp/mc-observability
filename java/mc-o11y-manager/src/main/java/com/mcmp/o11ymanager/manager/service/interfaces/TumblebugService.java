package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.enums.Agent;

public interface TumblebugService {

    String executeCommand(String nsId, String infraId, String nodeId, String command);

    boolean isConnectedVM(String nsId, String infraId, String nodeId);

    TumblebugInfra.Node getNode(String nsId, String infraId, String nodeId);

    boolean isServiceActive(String nsId, String infraId, String nodeId, Agent agent);

    String restart(String nsId, String infraId, String nodeId, Agent agent);
}
