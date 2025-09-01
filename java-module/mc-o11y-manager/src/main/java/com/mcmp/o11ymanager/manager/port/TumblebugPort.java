package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;

public interface TumblebugPort {

  TumblebugMCI.Vm getVM(String nsId, String mciId, String targetId);

  TumblebugSshKey getSshKey(String nsId, String sshKeyId);

  TumblebugNS getNSList();

  TumblebugMCI getMCIList(String nsId, String mciId);

  Object sendCommand(String nsId, String mciId, String targetId, TumblebugCmd command);

}
