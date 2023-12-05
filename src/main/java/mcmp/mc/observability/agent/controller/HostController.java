package mcmp.mc.observability.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.dto.PageableResBody;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.model.HostInfo;
import mcmp.mc.observability.agent.model.dto.HostUpdateDTO;
import mcmp.mc.observability.agent.service.HostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1 + "/host")
public class HostController {

    private final HostService hostService;

    @Operation(summary = "Get Host all list")
    @GetMapping("")
    public ResBody<PageableResBody<List<HostInfo>>> list(@Parameter(hidden = true) PageableReqBody<HostInfo> req) {
        if( req.getData() == null ) req.setData(new HostInfo());

        ResBody<PageableResBody<List<HostInfo>>> res = new ResBody<>();
        res.setData(hostService.getList(req));
        return res;
    }

    @Operation(summary = "Update request Host")
    @PutMapping("/{hostSeq}")
    public ResBody<?> update(@PathVariable("hostSeq") Long seq, @RequestBody HostUpdateDTO hostInfo) {
        hostInfo.setSeq(seq);
        return hostService.updateHost(hostInfo);
    }

    @Operation(hidden = true)
    @GetMapping("/{hostSeq}")
    public ResBody<HostInfo> detail(@PathVariable("hostSeq") Long seq) {
        return hostService.getDetail(new ResBody<>(), seq);
    }

    @Operation(summary = "Update request Host monitoring state on/off")
    @PutMapping("/{hostSeq}/turnMonitoringYn")
    public ResBody<?> turnMonitoringYn(@PathVariable("hostSeq") Long seq) {
        return hostService.turnMonitoringYn(seq);
    }

    @Operation(summary = "Update request Host all config")
    @PutMapping("/{hostSeq}/synchronize")
    public ResBody<?> synchronizeAll(@PathVariable("hostSeq") Long hostSeq) {
        return hostService.synchronizeAll(hostSeq);
    }
}