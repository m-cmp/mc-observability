package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "trigger-manager", url = "${feign.agent-manager.url:}")
public interface TriggerClient {

    @GetMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH + "/history")
    Object getTriggerHistory(@RequestParam Long policySeq);
    @GetMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH)
    Object getTriggerPolicy();
    @PostMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH)
    Object insertTriggerPolicy(@RequestBody Object body);
    @PatchMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH + "/{policySeq}")
    Object updateTriggerPolicy(@PathVariable Long policySeq, @RequestBody Object body);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH + "/{policySeq}")
    Object deleteTriggerPolicy(@PathVariable Long policySeq);
    @GetMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH + "/target")
    Object getTriggerPolicyTarget(@RequestParam Long policySeq);
    @PutMapping(Constants.PREFIX_V1 + Constants.TRIGGER_POLICY_PATH + "/{policySeq}/target")
    Object updateTriggerPolicyTarget(@PathVariable Long policySeq, @RequestBody Object body);

    @GetMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/email")
    Object getTriggerAlertEmail(@PathVariable Long policySeq);
    @PostMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/email")
    Object insertTriggerAlertEmail(@PathVariable Long policySeq, @RequestBody Object body);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/email/{seq}")
    Object deleteTriggerAlertEmail(@PathVariable Long policySeq, @PathVariable Long seq);
    @GetMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/slack")
    Object getTriggerAlertSlack(@PathVariable Long policySeq);
    @PostMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/slack")
    Object insertTriggerAlertSlack(@PathVariable Long policySeq, @RequestBody Object body);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.TRIGGER_ALERT_PATH + "/slack/{seq}")
    Object deleteTriggerAlertSlack(@PathVariable Long policySeq, @PathVariable Long seq);
}
