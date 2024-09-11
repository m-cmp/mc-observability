package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "trigger-manager", url = "${feign.agent-manager.url:}")
public interface TriggerClient {

    @GetMapping((Constants.TRIGGER))
    Object getPolicyList();

    @PostMapping(Constants.TRIGGER)
    Object createPolicy(@RequestBody Object triggerPolicyCreateDto);

    @PatchMapping(Constants.TRIGGER + "/{policySeq}")
    Object updatePolicy(@PathVariable Long policySeq, @RequestBody Object triggerPolicyUpdateDto);

    @DeleteMapping(Constants.TRIGGER + "/{policySeq}")
    Object deletePolicy(@PathVariable Long policySeq);

    @GetMapping(Constants.TRIGGER + "/target")
    Object getTriggerTargetList(@RequestParam Long policySeq);

    @PutMapping(Constants.TRIGGER + "/{policySeq}/target")
    Object setTriggerTarget(@PathVariable Long policySeq, @RequestBody Object targets);

    @GetMapping(Constants.TRIGGER + "/history")
    Object getTriggerHistoryList(@RequestParam Long policySeq);

    @GetMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/email")
    Object getTriggerEmailUserList(@PathVariable Long policySeq);

    @PostMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/email")
    Object createTriggerEmailUser(@PathVariable Long policySeq, @RequestBody Object triggerEmailUserCreateDto);

    @DeleteMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/email/{seq}")
    Object deleteTriggerEmailUser(@PathVariable Long policySeq, @PathVariable Long seq);

    @GetMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/slack")
    Object getTriggerSlackUserList(@PathVariable Long policySeq);

    @PostMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/slack")
    Object createTriggerSlackUser(@PathVariable Long policySeq, @RequestBody Object triggerSlackUserCreateDto);

    @DeleteMapping(Constants.TRIGGER + Constants.TRIGGER_ALERT + "/slack/{seq}")
    Object deleteTriggerSlackUser(@PathVariable Long policySeq, @PathVariable Long seq);
}
