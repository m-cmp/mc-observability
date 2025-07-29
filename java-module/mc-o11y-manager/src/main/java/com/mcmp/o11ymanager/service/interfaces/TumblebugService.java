package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.Agent;

public interface TumblebugService {



  String executeCommand(String nsId, String mciId, String vmId, String command);

  boolean isConnectedVM(String nsId, String mciId, String vmId);

  TumblebugMCI.Vm getVm(String nsId, String mciId, String vmId);

  boolean isServiceActive(String nsId, String mciId, String vmId, Agent agent);

}
