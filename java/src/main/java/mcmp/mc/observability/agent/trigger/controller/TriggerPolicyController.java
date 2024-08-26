package mcmp.mc.observability.agent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyUpdateDto;
import mcmp.mc.observability.agent.trigger.service.TriggerPolicyService;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/policy")
public class TriggerPolicyController {

    private final TriggerPolicyService triggerPolicyService;

    @ApiOperation(value = "Get Trigger Policy all list")
    @Base64Encode
    @GetMapping
    public ResBody<List<TriggerPolicyInfo>> list() {
        ResBody<List<TriggerPolicyInfo>> res = new ResBody<>();
        res.setData(triggerPolicyService.getList());
        return res;
    }

    @ApiOperation(value = "")
    @Base64Encode
    @GetMapping("/{policySeq}")
    public ResBody<TriggerPolicyInfo> detail(@PathVariable("policySeq") Long seq) {
        return triggerPolicyService.getDetail(new ResBody<>(), seq);
    }

    @ApiOperation(value = "Create request Trigger Policy")
    @Base64Decode(TriggerPolicyCreateDto.class)
    @PostMapping
    public ResBody<Void> create(@RequestBody TriggerPolicyCreateDto triggerPolicyCreateDto) {
        return triggerPolicyService.createPolicy(triggerPolicyCreateDto);
    }

    @ApiOperation(value = "Update request Trigger Policy")
    @Base64Decode(TriggerPolicyUpdateDto.class)
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
