package mcmp.mc.observability.mco11yagent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.trigger.common.Constants;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Encode;
import mcmp.mc.observability.mco11yagent.trigger.model.ResBody;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerHistoryInfo;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/history")
public class TriggerHistoryController {

    private final TriggerHistoryService triggerHistoryService;

    @ApiOperation(value = "Get Trigger History all list")
    @TriggerBase64Encode
    @GetMapping
    public ResBody<List<TriggerHistoryInfo>> list(@RequestParam("policySeq") Long policySeq) {
        ResBody<List<TriggerHistoryInfo>> res = new ResBody<>();
        res.setData(triggerHistoryService.getList(policySeq));
        return res;
    }

    @ApiOperation(value = "Get Trigger History detail", hidden = true)
    @TriggerBase64Encode
    @GetMapping("/{historySeq}")
    public ResBody<TriggerHistoryInfo> detail(@PathVariable("historySeq") Long seq) {
        return triggerHistoryService.getDetail(new ResBody<>(), seq);
    }
}
