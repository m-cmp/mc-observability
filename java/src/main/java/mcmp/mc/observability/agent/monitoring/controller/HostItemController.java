package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.HostItemInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.HostItemCreateDTO;
import mcmp.mc.observability.agent.monitoring.model.dto.HostItemUpdateDTO;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.HostItemService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(Constants.MONITORING_URI + "/host/{hostSeq}/item")
@RequiredArgsConstructor
public class HostItemController {

    private final HostItemService hostItemService;

    @ApiOperation(value = "Get Host item all list")
    @Base64Encode
    @GetMapping("")
    public ResBody<PageableResBody<HostItemInfo>> list(@PathVariable("hostSeq") Long hostSeq, @ApiIgnore PageableReqBody<HostItemInfo> req) {
        req.setData(new HostItemInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody<PageableResBody<HostItemInfo>> res = new ResBody<>();
        res.setData(hostItemService.getList(req));
        return res;
    }

    @ApiOperation(value = "Create request Host item")
    @Base64Decode(HostItemCreateDTO.class)
    @PostMapping("")
    public ResBody<Void> create(@PathVariable("hostSeq") Long hostSeq, @RequestBody HostItemCreateDTO info) {
        info.setHostSeq(hostSeq);
        return hostItemService.insertItem(info);
    }

    @ApiOperation(value = "Update request Host item")
    @Base64Decode(HostItemUpdateDTO.class)
    @PutMapping("/{itemSeq}")
    public ResBody<Void> update(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq, @RequestBody HostItemUpdateDTO info) {
        info.setHostSeq(hostSeq);
        info.setSeq(seq);
        return hostItemService.updateItem(info);
    }

    @ApiOperation(value = "Delete request Host item")
    @DeleteMapping("/{itemSeq}")
    public ResBody<Void> delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.deleteItem(hostSeq, seq);
    }

    @ApiOperation(value = "", hidden = true)
    @Base64Encode
    @GetMapping("/{itemSeq}")
    public ResBody<HostItemInfo> detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.getDetail(new ResBody<>(), hostSeq,  seq);
    }

    @ApiOperation(value = "Update request Host item monitoring state on/off")
    @PutMapping("/{itemSeq}/turnMonitoringYn")
    public ResBody<Void> turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.turnMonitoringYn(hostSeq, seq);
    }

}