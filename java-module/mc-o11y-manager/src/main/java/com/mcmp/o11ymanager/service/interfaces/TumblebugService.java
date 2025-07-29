package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.Agent;

public interface TumblebugService {

  String executeCommand(String nsId, String mciId, String targetId, String command, String userName);

  boolean isConnectedVM(String nsId, String mciId, String targetId, String userName);

  TumblebugMCI.Vm getVm(String nsId, String mciId, String targetId);

  boolean isServiceActive(String nsId, String mciId, String serviceName, String userName, Agent agent);

}
