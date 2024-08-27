package mcmp.mc.observability.mco11yagent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.trigger.common.TriggerConstants;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64Encode;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerResBody;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerHistoryInfo;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(TriggerConstants.TRIGGER_URI + "/history")
public class TriggerHistoryController {

    private final TriggerHistoryService triggerHistoryService;

    @ApiOperation(value = "Get Trigger History all list")
    @TriggerBase64Encode
    @GetMapping
    public TriggerResBody<List<TriggerHistoryInfo>> list(@RequestParam("policySeq") Long policySeq) {
        TriggerResBody<List<TriggerHistoryInfo>> res = new TriggerResBody<>();
        res.setData(triggerHistoryService.getList(policySeq));
        return res;
    }

    @ApiOperation(value = "Get Trigger History detail", hidden = true)
    @TriggerBase64Encode
    @GetMapping("/{historySeq}")
    public TriggerResBody<TriggerHistoryInfo> detail(@PathVariable("historySeq") Long seq) {
        return triggerHistoryService.getDetail(new TriggerResBody<>(), seq);
    }
}
