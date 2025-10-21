package com.mcmp.o11ymanager.manager.infrastructure.trigger;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.ManagerPort;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManagerAdapter implements ManagerPort {

    private final VMService vmService;
    private final InfluxDbService influxDbService;

    @Override
    public String getInfluxUid(String nsId, String vmScope, String vmId) {

        Long influxId;

        // 1. Check whether vmScope is "mci_id" or "vm_id"
        if ("mci".equalsIgnoreCase(vmScope)) {
            final String mciId = vmId;

            // 2. Map influx id using the combination of ns id and mci id
            List<VMDTO> vms = vmService.getByNsMci(nsId, mciId);
            influxId =
                    vms.stream()
                            .map(VMDTO::getInfluxSeq)
                            .filter(Objects::nonNull)
                            .findFirst() // Can choose first/latest/minimum based on rules
                            .orElseGet(() -> influxDbService.resolveInfluxDb(nsId, mciId));

        } else if ("vm".equalsIgnoreCase(vmScope)) {
            // 3. Map mci id using ns and vm
            VMDTO t = vmService.getByNsVm(nsId, vmId);

            influxId = t.getInfluxSeq();
            if (influxId == null) {
                String mciId = t.getMciId();
                if (mciId == null || mciId.isBlank()) {
                    throw new IllegalStateException(
                            "mciId not found for vm: ns=" + nsId + ", vmId=" + vmId);
                }
                influxId = influxDbService.resolveInfluxDb(nsId, mciId);
            }

        } else {
            throw new IllegalArgumentException("unknown vmScope: " + vmScope);
        }

        String uid = influxDbService.get(influxId).getUid();
        log.info(
                "=====================================================INFLUX UID : {}===============================================",
                uid);

        return uid;
    }
}
