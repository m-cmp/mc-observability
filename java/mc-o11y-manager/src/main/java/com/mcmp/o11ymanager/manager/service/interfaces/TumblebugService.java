package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.enums.Agent;

public interface TumblebugService {

    String executeCommand(String nsId, String mciId, String vmId, String command);

    boolean isConnectedVM(String nsId, String mciId, String vmId);

    TumblebugMCI.Vm getVm(String nsId, String mciId, String vmId);

    boolean isServiceActive(String nsId, String mciId, String vmId, Agent agent);

    String restart(String nsId, String mciId, String vmId, Agent agent);
}
