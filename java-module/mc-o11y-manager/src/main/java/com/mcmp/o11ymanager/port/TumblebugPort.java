package com.mcmp.o11ymanager.port;

import com.mcmp.o11ymanager.dto.tumblebug.*;

public interface TumblebugPort {

  TumblebugMCI.Vm getVM(String nsId, String mciId, String targetId);

  TumblebugSshKey getSshKey(String nsId, String sshKeyId);

  TumblebugNS getNSList();

  TumblebugMCI getMCIList(String nsId, String mciId);

  Object sendCommand(String nsId, String mciId, String targetId, TumblebugCmd command);

}
