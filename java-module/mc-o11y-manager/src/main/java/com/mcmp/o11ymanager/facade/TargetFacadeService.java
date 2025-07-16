package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.dto.tumblebug.SshKey;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugSshKeyList;
import com.mcmp.o11ymanager.port.TumblebugPort;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFacadeService {

  private final TargetService targetService;
  private final TumblebugPort tumblebugPort;

  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, targetId);

    return TargetDTO.builder()
        .id(vm.getId())
        .name(vm.getName())
        .aliasName(vm.getAliasName())
        .description(vm.getDescription())
        .nsId(nsId)
        .mciId(mciId)
        .state(vm.getState())
        .build();

  }

  public TargetDTO getTargetsNsMci(String nsId, String mciId) {
    return targetService.getByNsMci(nsId, mciId);
  }

  public List<TargetDTO> getTargets() {
    return targetService.list();
  }

  public TargetDTO postTarget(TargetRegisterDTO dto) {

    TumblebugMCI.Vm vm = tumblebugPort.getVM(dto.getNsId(), dto.getMciId(), dto.getId());

    TumblebugSshKeyList sshKeyList = tumblebugPort.getSshKeyList(dto.getNsId());


    String privateKey = sshKeyList.getSshKey().stream()
        .filter(k -> k.getId().equals(vm.getSshKeyId()))
        .map(SshKey::getPrivateKey)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("SSH private key not found"));


    TargetRegisterDTO.AccessInfoDTO accessInfo = TargetRegisterDTO.AccessInfoDTO.builder()
        .ip(vm.getPublicIP())
        .port(Integer.parseInt(vm.getSshPort()))
        .user(vm.getVmUserName())
        .sshKey(privateKey)
        .build();

    dto.setAccessInfo(accessInfo);


    return targetService.post(dto);
  }

  public TargetDTO putTarget(String targetId, String nsId, String mciId, TargetUpdateDTO dto) {
    return targetService.put(targetId, nsId, mciId, dto);
  }

  public void deleteTarget(String targetId, String nsId, String mciId) {
    targetService.delete(targetId, nsId, mciId);
  }


  public List<TumblebugNS.NS> getNamespaceList() {
    return tumblebugPort.getNSList().getNs();
  }

}
