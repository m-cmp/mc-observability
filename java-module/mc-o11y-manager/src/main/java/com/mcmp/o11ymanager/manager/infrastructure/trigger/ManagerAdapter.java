package com.mcmp.o11ymanager.manager.infrastructure.trigger;

import com.mcmp.o11ymanager.manager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.manager.repository.InfluxJpaRepository;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.manager.service.interfaces.TargetService;
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

    private final TargetService targetService;
    private final InfluxDbService influxDbService;
    private final InfluxJpaRepository influxJpaRepository;

    @Override
    public String getInfluxUid(String nsId, String targetScope, String targetId) {

        Long influxId;

        // 1.targetScope가 "mci_id"인지 "vm_id" 인지
        if ("mci".equalsIgnoreCase(targetScope)) {
            final String mciId = targetId;

            // 2. ns id + mci id  조합으로 influx id 매핑
            List<TargetDTO> targets = targetService.getByNsMci(nsId, mciId);
            influxId =
                    targets.stream()
                            .map(TargetDTO::getInfluxSeq)
                            .filter(Objects::nonNull)
                            .findFirst() // 규칙에 따라 first/최신/최소 등 선택 가능
                            .orElseGet(() -> influxDbService.resolveInfluxDb(nsId, mciId));

        } else if ("vm".equalsIgnoreCase(targetScope)) {
            // 3. ns + vm 으로 mci id 매핑
            TargetDTO t = targetService.getByNsVm(nsId, targetId);

            influxId = t.getInfluxSeq();
            if (influxId == null) {
                String mciId = t.getMciId();
                if (mciId == null || mciId.isBlank()) {
                    throw new IllegalStateException(
                            "mciId not found for target: ns=" + nsId + ", vmId=" + targetId);
                }
                influxId = influxDbService.resolveInfluxDb(nsId, mciId);
            }

        } else {
            throw new IllegalArgumentException("unknown targetScope: " + targetScope);
        }

        String uid = influxDbService.get(influxId).getUid();
        log.info(
                "=====================================================INFLUX UID : {}===============================================",
                uid);

        return uid;
    }
}
