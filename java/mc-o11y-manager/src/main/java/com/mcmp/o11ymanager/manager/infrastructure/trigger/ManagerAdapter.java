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
    public String getInfluxUid(String nsId, String vmScope, String nodeId) {

        Long influxId;

        // 1. Check whether vmScope is "infra_id" or "node_id"
        if ("infra".equalsIgnoreCase(vmScope)) {
            final String infraId = nodeId;

            // 2. Map influx id using the combination of ns id and infra id
            List<VMDTO> vms = vmService.getByNsMci(nsId, infraId);
            influxId =
                    vms.stream()
                            .map(VMDTO::getInfluxSeq)
                            .filter(Objects::nonNull)
                            .findFirst() // Can choose first/latest/minimum based on rules
                            .orElseGet(() -> influxDbService.resolveInfluxDb(nsId, infraId));

        } else if ("node".equalsIgnoreCase(vmScope)) {
            // 3. Map infra id using ns and node
            VMDTO t = vmService.getByNsVm(nsId, nodeId);

            influxId = t.getInfluxSeq();
            if (influxId == null) {
                String infraId = t.getInfraId();
                if (infraId == null || infraId.isBlank()) {
                    throw new IllegalStateException(
                            "infraId not found for vm: ns=" + nsId + ", nodeId=" + nodeId);
                }
                influxId = influxDbService.resolveInfluxDb(nsId, infraId);
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
