package mcmp.mc.observability.agent.trigger.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.trigger.model.TriggerSlackUserInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;
import mcmp.mc.observability.agent.trigger.service.TriggerPolicyAlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/policy/{policySeq}" + "/alert")
public class TriggerPolicyAlertController {

    private final TriggerPolicyAlertService triggerPolicyAlertService;

    @Base64Encode
    @GetMapping("/slack")
    public ResBody<List<TriggerSlackUserInfo>> getSlackUserList(@PathVariable(value = "policySeq") Long policySeq) {
        ResBody<List<TriggerSlackUserInfo>> res = new ResBody<>();
        res.setData(triggerPolicyAlertService.getSlackUserList(policySeq));
        return res;
    }

    @Base64Decode(TriggerSlackUserCreateDto.class)
    @PostMapping("/slack")
    public ResBody<Void> createSlackUser(@PathVariable("policySeq") Long policySeq, @RequestBody TriggerSlackUserCreateDto dto) {
        dto.setPolicySeq(policySeq);
        return triggerPolicyAlertService.createSlackUser(dto);
    }

    @DeleteMapping("/slack/{seq}")
    public ResBody<Void> deleteSlackUser(@PathVariable("policySeq") Long policySeq, @PathVariable("seq") Long seq) {
        return triggerPolicyAlertService.deleteSlackUser(seq);
    }

    @PostMapping("/send")
    public ResBody<Void> sendMessageToSlack(@PathVariable("policySeq") Long policySeq, @RequestParam Long seq, @RequestParam String message) {
        return triggerPolicyAlertService.sendSlack(seq, message);
    }

}
