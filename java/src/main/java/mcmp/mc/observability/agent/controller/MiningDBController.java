package mcmp.mc.observability.agent.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.MiningDBInfo;
import mcmp.mc.observability.agent.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.service.MiningDBService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(Constants.PREFIX_V1 + "/miningdb")
@RequiredArgsConstructor
public class MiningDBController {

    private final MiningDBService miningDBService;

    @ApiOperation(value = "Get Mining DB info")
    @GetMapping
    public ResBody<MiningDBInfo> detail() {
        return miningDBService.detail(new ResBody<>());
    }

    @ApiOperation(value = "Create Mining DB info")
    @PutMapping
    public ResBody<Void> setMiningDB(@RequestBody MiningDBSetDTO info) {
        return miningDBService.setMiningDB(info);
    }

}
