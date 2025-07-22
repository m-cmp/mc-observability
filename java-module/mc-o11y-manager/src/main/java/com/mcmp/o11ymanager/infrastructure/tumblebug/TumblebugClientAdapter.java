package com.mcmp.o11ymanager.infrastructure.tumblebug;

import com.mcmp.o11ymanager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugSshKeyList;
import com.mcmp.o11ymanager.port.TumblebugPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TumblebugClientAdapter implements TumblebugPort {

  private final TumblebugClient tumblebugClient;

  @Override
  public TumblebugMCI.Vm getVM(String nsId, String mciId, String vmId) {
    return tumblebugClient.getVM(nsId, mciId, vmId);
  }

  @Override
  public TumblebugSshKeyList getSshKeyList(String nsId) {
    return tumblebugClient.getSshKeyList(nsId);
  }

  @Override
  public TumblebugNS getNSList() {
    return tumblebugClient.getNSList();
  }

  @Override
  public TumblebugMCI getMCIList(String nsId, String mciId) {
    return tumblebugClient.getMCIList(nsId, mciId);
  }

  @Override
  public Object sendCommand(String nsId, String mciId, TumblebugCmd command) {
    return tumblebugClient.sendCommand(nsId, mciId, command);
  }

}
