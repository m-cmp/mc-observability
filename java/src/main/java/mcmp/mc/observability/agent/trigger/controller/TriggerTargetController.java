package mcmp.mc.observability.agent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetInfo;
import mcmp.mc.observability.agent.trigger.model.dto.ManageTriggerTargetDto;
import mcmp.mc.observability.agent.trigger.service.TriggerTargetService;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/target")
public class TriggerTargetController {

    private final TriggerTargetService triggerTargetService;


    @ApiOperation(value = "Get Trigger Target all list")
    @Base64Encode
    @GetMapping
    public ResBody<List<TriggerTargetInfo>> list(@RequestParam("policySeq") Long policySeq) {
        ResBody<List<TriggerTargetInfo>> res = new ResBody<>();
        res.setData(triggerTargetService.getList(policySeq));
        return res;
    }

    @ApiOperation(value = "Get Trigger Target detail", hidden = true)
    @Base64Encode
    @GetMapping("/{targetSeq}")
    public ResBody<TriggerTargetInfo> detail(@PathVariable("targetSeq") Long seq) {
        return triggerTargetService.getDetail(new ResBody<>(), seq);
    }

    @ApiOperation(value = "Set trigger target")
    @PutMapping("/{policySeq}")
    public ResBody<Void> setTargets(@PathVariable("policySeq") Long policySeq,
            @RequestBody List<ManageTriggerTargetDto> targets) {
        return triggerTargetService.setTargets(policySeq, targets);
    }
}
