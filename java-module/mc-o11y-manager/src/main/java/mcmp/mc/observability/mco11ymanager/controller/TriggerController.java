package mcmp.mc.observability.mco11ymanager.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.client.TriggerClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1)
public class TriggerController {
    private final TriggerClient triggerClient;

    // trigger policy api
    @GetMapping(Constants.TRIGGER_POLICY_PATH + "/history")
    public Object getTriggerHistory(@RequestParam Long policySeq) {
        return triggerClient.getTriggerHistory(policySeq);
    }
    @GetMapping(Constants.TRIGGER_POLICY_PATH)
    public Object getTriggerPolicy() {
        return triggerClient.getTriggerPolicy();
    }
    @PostMapping(Constants.TRIGGER_POLICY_PATH)
    public Object insertTriggerPolicy(@RequestBody Object body) {
        return triggerClient.insertTriggerPolicy(body);
    }
    @PatchMapping(Constants.TRIGGER_POLICY_PATH + "/{policySeq}")
    public Object updateTriggerPolicy(@PathVariable Long policySeq, @RequestBody Object body) {
        return triggerClient.updateTriggerPolicy(policySeq, body);
    }
    @DeleteMapping(Constants.TRIGGER_POLICY_PATH + "/{policySeq}")
    public Object deleteTriggerPolicy(@PathVariable Long policySeq) {
        return triggerClient.deleteTriggerPolicy(policySeq);
    }
    @GetMapping(Constants.TRIGGER_POLICY_PATH + "/target")
    public Object getTriggerPolicyTarget(@RequestParam Long policySeq) {
        return triggerClient.getTriggerPolicyTarget(policySeq);
    }
    @PutMapping(Constants.TRIGGER_POLICY_PATH + "/{policySeq}/target")
    public Object updateTriggerPolicyTarget(@PathVariable Long policySeq, @RequestBody Object body) {
        return triggerClient.updateTriggerPolicyTarget(policySeq, body);
    }

    // trigger alert api
    @GetMapping(Constants.TRIGGER_ALERT_PATH + "/email")
    public Object getTriggerAlertEmail(@PathVariable Long policySeq) {
        return triggerClient.getTriggerAlertEmail(policySeq);
    }
    @PostMapping(Constants.TRIGGER_ALERT_PATH + "/email")
    public Object insertTriggerAlertEmail(@PathVariable Long policySeq, @RequestBody Object body) {
        return triggerClient.insertTriggerAlertEmail(policySeq, body);
    }
    @DeleteMapping(Constants.TRIGGER_ALERT_PATH + "/email/{seq}")
    public Object deleteTriggerAlertEmail(@PathVariable Long policySeq, @PathVariable Long seq) {
        return triggerClient.deleteTriggerAlertEmail(policySeq, seq);
    }
    @GetMapping(Constants.TRIGGER_ALERT_PATH + "/slack")
    public Object getTriggerAlertSlack(@PathVariable Long policySeq) {
        return triggerClient.getTriggerAlertSlack(policySeq);
    }
    @PostMapping(Constants.TRIGGER_ALERT_PATH + "/slack")
    public Object insertTriggerAlertSlack(@PathVariable Long policySeq, @RequestBody Object body) {
        return triggerClient.insertTriggerAlertSlack(policySeq, body);
    }
    @DeleteMapping(Constants.TRIGGER_ALERT_PATH + "/slack/{seq}")
    public Object deleteTriggerAlertSlack(@PathVariable Long policySeq, @PathVariable Long seq) {
        return triggerClient.deleteTriggerAlertSlack(policySeq, seq);
    }
}
