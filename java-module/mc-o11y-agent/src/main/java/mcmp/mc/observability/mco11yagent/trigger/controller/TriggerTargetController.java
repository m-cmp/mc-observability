package mcmp.mc.observability.mco11yagent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.trigger.common.TriggerConstants;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Encode;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerTargetInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.ManageTriggerTargetDto;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerTargetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(TriggerConstants.TRIGGER_URI)
public class TriggerTargetController {

    private final TriggerTargetService triggerTargetService;


    @ApiOperation(value = "Get Trigger Target all list")
    @TriggerBase64Encode
    @GetMapping("/target")
    public ResBody<List<TriggerTargetInfo>> list(@RequestParam("policySeq") Long policySeq) {
        ResBody<List<TriggerTargetInfo>> res = new ResBody<>();
        res.setData(triggerTargetService.getList(policySeq));
        return res;
    }

    @ApiOperation(value = "Get Trigger Target detail", hidden = true)
    @TriggerBase64Encode
    @GetMapping("/target/{targetSeq}")
    public ResBody<TriggerTargetInfo> detail(@PathVariable("targetSeq") Long seq) {
        return triggerTargetService.getDetail(new ResBody<>(), seq);
    }

    @ApiOperation(value = "Set trigger target")
    @PutMapping("/{policySeq}/target")
    public ResBody<Void> setTargets(@PathVariable("policySeq") Long policySeq,
                                           @RequestBody List<ManageTriggerTargetDto> targets) {
        return triggerTargetService.setTargets(policySeq, targets);
    }
}
