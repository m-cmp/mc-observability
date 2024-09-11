package mcmp.mc.observability.mco11yagent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Decode;
import mcmp.mc.observability.mco11yagent.trigger.common.TriggerConstants;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Encode;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerResBody;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerEmailUserInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerSlackUserInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerEmailUserCreateDto;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerSlackUserCreateDto;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerPolicyAlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(TriggerConstants.TRIGGER_URI + "/policy/{policySeq}" + "/alert")
public class TriggerPolicyAlertController {

    private final TriggerPolicyAlertService triggerPolicyAlertService;

    @ApiOperation(value = "Get Trigger Alert Slack User all list")
    @TriggerBase64Encode
    @GetMapping("/slack")
    public TriggerResBody<List<TriggerSlackUserInfo>> getSlackUserList(@PathVariable(value = "policySeq") Long policySeq) {
        TriggerResBody<List<TriggerSlackUserInfo>> res = new TriggerResBody<>();
        res.setData(triggerPolicyAlertService.getSlackUserList(policySeq));
        return res;
    }

    @ApiOperation(value = "Create request Trigger Alert Slack User")
    @TriggerBase64Decode(TriggerSlackUserCreateDto.class)
    @PostMapping("/slack")
    public TriggerResBody<Void> createSlackUser(@PathVariable("policySeq") Long policySeq, @RequestBody TriggerSlackUserCreateDto dto) {
        dto.setPolicySeq(policySeq);
        return triggerPolicyAlertService.createSlackUser(dto);
    }

    @ApiOperation(value = "Delete Request Trigger Alert Slack User")
    @DeleteMapping("/slack/{seq}")
    public TriggerResBody<Void> deleteSlackUser(@PathVariable("policySeq") Long policySeq, @PathVariable("seq") Long seq) {
        return triggerPolicyAlertService.deleteSlackUser(seq);
    }

    @ApiOperation(value = "Get Trigger Alert Email User all list")
    @TriggerBase64Encode
    @GetMapping("/email")
    public TriggerResBody<List<TriggerEmailUserInfo>> getEmailUserList(@PathVariable(value = "policySeq") Long policySeq) {
        TriggerResBody<List<TriggerEmailUserInfo>> res = new TriggerResBody<>();
        res.setData(triggerPolicyAlertService.getEmailUserList(policySeq));
        return res;
    }

    @ApiOperation(value = "Create request Trigger Alert Email User")
    @TriggerBase64Decode(TriggerSlackUserCreateDto.class)
    @PostMapping("/email")
    public TriggerResBody<Void> createEmailUser(@PathVariable("policySeq") Long policySeq, @RequestBody TriggerEmailUserCreateDto dto) {
        dto.setPolicySeq(policySeq);
        return triggerPolicyAlertService.createEmailUser(dto);
    }

    @ApiOperation(value = "Delete Request Trigger Alert Email User")
    @DeleteMapping("/email/{seq}")
    public TriggerResBody<Void> deleteEmailUser(@PathVariable("policySeq") Long policySeq, @PathVariable("seq") Long seq) {
        return triggerPolicyAlertService.deleteEmailUser(seq);
    }
}
