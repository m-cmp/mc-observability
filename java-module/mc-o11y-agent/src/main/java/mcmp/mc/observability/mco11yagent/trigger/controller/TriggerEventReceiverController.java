package mcmp.mc.observability.mco11yagent.trigger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.trigger.common.TriggerConstants;
import mcmp.mc.observability.mco11yagent.trigger.model.KapacitorAlertInfo;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerEventHandlerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(TriggerConstants.TRIGGER_URI + "/receiver")
public class TriggerEventReceiverController {

    private final TriggerEventHandlerService triggerEventHandlerService;

    @PostMapping
    public void getTriggerEvent(@RequestBody String data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            KapacitorAlertInfo kapacitorAlertInfo = objectMapper.readValue(data, KapacitorAlertInfo.class);
            triggerEventHandlerService.checkTriggerTarget(kapacitorAlertInfo);
        } catch (Exception e) {
            log.error("Failed to record trigger event history. Error : {}", e.getMessage());
        }
    }

}
