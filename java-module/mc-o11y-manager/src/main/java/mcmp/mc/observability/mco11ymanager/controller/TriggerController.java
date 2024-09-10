package mcmp.mc.observability.mco11ymanager.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.client.TriggerClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER)
public class TriggerController {

    private final TriggerClient triggerClient;

    // trigger policy
    @GetMapping
    public Object getPolicyList() {
        return triggerClient.getPolicyList();
    }
    @PostMapping
    public Object createPolicy(@RequestBody Object triggerPolicyCreateDto) {
        return triggerClient.createPolicy(triggerPolicyCreateDto);
    }
    @PatchMapping("/{policySeq}")
    public Object updatePolicy(@PathVariable Long policySeq, @RequestBody Object triggerPolicyUpdateDto) {
        return triggerClient.updatePolicy(policySeq, triggerPolicyUpdateDto);
    }
    @DeleteMapping("/{policySeq}")
    public Object deletePolicy(@PathVariable Long policySeq) {
        return triggerClient.deletePolicy(policySeq);
    }
    // trigger target
    @GetMapping("/target")
    public Object getTriggerTargetList(@RequestParam Long policySeq) {
        return triggerClient.getTriggerTargetList(policySeq);
    }
    @PutMapping("/{policySeq}/target")
    public Object setTriggerTarget(@PathVariable Long policySeq, @RequestBody Object targets) {
        return triggerClient.setTriggerTarget(policySeq, targets);
    }

    // trigger history
    @GetMapping("/history")
    public Object getTriggerHistoryList(@RequestParam Long policySeq) {
        return triggerClient.getTriggerHistoryList(policySeq);
    }

    // trigger event handler
    @GetMapping(Constants.TRIGGER_ALERT + "/email")
    Object getTriggerEmailUserList(@PathVariable Long policySeq) {
        return triggerClient.getTriggerEmailUserList(policySeq);
    }
    @PostMapping(Constants.TRIGGER_ALERT + "/email")
    Object createTriggerEmailUser(@PathVariable Long policySeq, @RequestBody Object triggerEmailUserCreateDto) {
        return triggerClient.createTriggerEmailUser(policySeq, triggerEmailUserCreateDto);
    }

    @DeleteMapping(Constants.TRIGGER_ALERT + "/email/{seq}")
    Object deleteTriggerEmailUser(@PathVariable Long policySeq, @PathVariable Long seq) {
        return triggerClient.deleteTriggerEmailUser(policySeq, seq);
    }

    @GetMapping(Constants.TRIGGER_ALERT + "/slack")
    Object getTriggerSlackUserList(@PathVariable Long policySeq) {
        return triggerClient.getTriggerSlackUserList(policySeq);
    }
    @PostMapping(Constants.TRIGGER_ALERT + "/slack")
    Object createTriggerSlackUser(@PathVariable Long policySeq, @RequestBody Object triggerSlackUserCreateDto) {
        return triggerClient.createTriggerSlackUser(policySeq, triggerSlackUserCreateDto);
    }

    @DeleteMapping(Constants.TRIGGER_ALERT + "/slack/{seq}")
    Object deleteTriggerSlackUser(@PathVariable Long policySeq, @PathVariable Long seq) {
        return triggerClient.deleteTriggerSlackUser(policySeq, seq);
    }
}
