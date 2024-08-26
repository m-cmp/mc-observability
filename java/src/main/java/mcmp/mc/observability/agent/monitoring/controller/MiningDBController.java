package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.MiningDBInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.MiningDBService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(Constants.MONITORING_URI + "/miningdb")
@RequiredArgsConstructor
public class MiningDBController {

    private final MiningDBService miningDBService;

    @ApiOperation(value = "Get Mining DB info")
    @GetMapping
    public ResBody<MiningDBInfo> detail() {
        return miningDBService.detail();
    }

    @ApiOperation(value = "Update Mining DB info")
    @PutMapping
    public ResBody<Void> updateMiningDB(@RequestBody MiningDBSetDTO info) {
        return miningDBService.updateMiningDB(info);
    }

}
