package mcmp.mc.observability.mco11yagent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Decode;
import mcmp.mc.observability.mco11yagent.trigger.common.TriggerConstants;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Encode;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyUpdateDto;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerPolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(TriggerConstants.TRIGGER_URI)
public class TriggerPolicyController {

    private final TriggerPolicyService triggerPolicyService;

    @ApiOperation(value = "Get Trigger Policy all list")
    @TriggerBase64Encode
    @GetMapping
    public ResBody<List<TriggerPolicyInfo>> list() {
        ResBody<List<TriggerPolicyInfo>> res = new ResBody<>();
        res.setData(triggerPolicyService.getList());
        return res;
    }

    @ApiOperation(value = "")
    @TriggerBase64Encode
    @GetMapping("/{policySeq}")
    public ResBody<TriggerPolicyInfo> detail(@PathVariable("policySeq") Long seq) {
        return triggerPolicyService.getDetail(new ResBody<>(), seq);
    }

    @ApiOperation(value = "Create request Trigger Policy")
    @TriggerBase64Decode(TriggerPolicyCreateDto.class)
    @PostMapping
    public ResBody<Void> create(@RequestBody TriggerPolicyCreateDto triggerPolicyCreateDto) {
        return triggerPolicyService.createPolicy(triggerPolicyCreateDto);
    }

    @ApiOperation(value = "Update request Trigger Policy")
    @TriggerBase64Decode(TriggerPolicyUpdateDto.class)
    @PatchMapping("/{policySeq}")
    public ResBody<Void> update(@PathVariable("policySeq") Long seq, @RequestBody TriggerPolicyUpdateDto triggerPolicyUpdateDto) {
        triggerPolicyUpdateDto.setSeq(seq);
        return triggerPolicyService.updatePolicy(triggerPolicyUpdateDto);
    }

    @ApiOperation(value = "Delete Request Trigger Policy")
    @DeleteMapping("/{policySeq}")
    public ResBody<Void> delete(@PathVariable("policySeq") Long policySeq) {
        return triggerPolicyService.deletePolicy(policySeq);
    }

}
