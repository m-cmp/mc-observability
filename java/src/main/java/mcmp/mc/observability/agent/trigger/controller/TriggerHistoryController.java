package mcmp.mc.observability.agent.trigger.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.trigger.model.TriggerHistoryInfo;
import mcmp.mc.observability.agent.trigger.service.TriggerHistoryService;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.TRIGGER_URI + "/history")
public class TriggerHistoryController {

    private final TriggerHistoryService triggerHistoryService;

    @ApiOperation(value = "Get Trigger History all list")
    @Base64Encode
    @GetMapping
    public ResBody<PageableResBody<TriggerHistoryInfo>> list(@RequestParam("policySeq") Long policySeq, @ApiIgnore PageableReqBody<TriggerHistoryInfo> req) {
        if( req.getData() == null ) req.setData(new TriggerHistoryInfo());
        req.getData().setPolicySeq(policySeq);
        ResBody<PageableResBody<TriggerHistoryInfo>> res = new ResBody<>();
        res.setData(triggerHistoryService.getList(req));
        return res;
    }

    @ApiOperation(value = "Get Trigger History detail", hidden = true)
    @Base64Encode
    @GetMapping("/{historySeq}")
    public ResBody<TriggerHistoryInfo> detail(@PathVariable("historySeq") Long seq) {
        return triggerHistoryService.getDetail(new ResBody<>(), seq);
    }
}
