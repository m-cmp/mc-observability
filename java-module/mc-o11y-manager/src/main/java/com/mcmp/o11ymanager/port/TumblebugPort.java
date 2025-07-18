package com.mcmp.o11ymanager.port;

import com.mcmp.o11ymanager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugSshKeyList;

public interface TumblebugPort {

  TumblebugMCI.Vm getVM(String nsId, String mciId, String targetId);

  TumblebugSshKeyList getSshKeyList(String nsId);

  TumblebugNS getNSList();

  TumblebugMCI getMCIList(String nsId, String mciId);

  Object sendCommand(String nsId, String mciId, String subGroupId, String targetId, TumblebugCmd command);

}
