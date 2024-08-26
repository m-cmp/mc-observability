package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.HostUpdateDTO;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.HostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI + "/host")
public class HostController {

    private final HostService hostService;

    @ApiOperation(value = "Get Host all list")
    @Base64Encode
    @GetMapping("")
    public ResBody<PageableResBody<HostInfo>> list(@ApiIgnore PageableReqBody<HostInfo> req) {
        if( req.getData() == null ) req.setData(new HostInfo());

        ResBody<PageableResBody<HostInfo>> res = new ResBody<>();
        res.setData(hostService.getList(req));
        return res;
    }

    @ApiOperation(value = "Update request Host")
    @Base64Decode(HostUpdateDTO.class)
    @PutMapping("/{hostSeq}")
    public ResBody<Void> update(@PathVariable("hostSeq") Long seq, @RequestBody HostUpdateDTO hostInfo) {
        hostInfo.setSeq(seq);
        return hostService.updateHost(hostInfo);
    }

    @ApiOperation(value = "", hidden = true)
    @Base64Encode
    @GetMapping("/{hostSeq}")
    public ResBody<HostInfo> detail(@PathVariable("hostSeq") Long seq) {
        return hostService.getDetail(new ResBody<>(), seq);
    }

    @ApiOperation(value = "Update request Host monitoring state on/off")
    @PutMapping("/{hostSeq}/turnMonitoringYn")
    public ResBody<Void> turnMonitoringYn(@PathVariable("hostSeq") Long seq) {
        return hostService.turnMonitoringYn(seq);
    }

    @ApiOperation(value = "Update request Host all config")
    @PutMapping("/{hostSeq}/synchronize")
    public ResBody<Void> synchronizeAll(@PathVariable("hostSeq") Long hostSeq) {
        return hostService.synchronizeAll(hostSeq);
    }
}
