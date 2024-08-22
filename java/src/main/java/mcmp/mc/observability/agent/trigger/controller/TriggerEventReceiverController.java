package mcmp.mc.observability.agent.trigger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.trigger.model.KapacitorAlertInfo;
import mcmp.mc.observability.agent.trigger.service.TriggerEventHandlerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/receiver")
public class TriggerEventReceiverController {

    private final TriggerEventHandlerService triggerEventHandlerService;

    @PostMapping
    public void getTriggerEvent(@RequestBody String data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            KapacitorAlertInfo kapacitorAlertInfo = objectMapper.readValue(data, KapacitorAlertInfo.class);
            triggerEventHandlerService.checkTriggerTarget(kapacitorAlertInfo);
        } catch (Exception e) {

        }
    }

}
