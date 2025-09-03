package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.enums.Agent;

public interface TumblebugService {

    String executeCommand(String nsId, String mciId, String targetId, String command);

    boolean isConnectedVM(String nsId, String mciId, String targetId);

    TumblebugMCI.Vm getVm(String nsId, String mciId, String targetId);

    boolean isServiceActive(String nsId, String mciId, String targetId, Agent agent);

    String restart(String nsId, String mciId, String targetId, Agent agent);
}
