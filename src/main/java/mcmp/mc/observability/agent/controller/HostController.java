package mcmp.mc.observability.agent.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.dto.PageableReqBody;
import mcmp.mc.observability.agent.dto.PageableResBody;
import mcmp.mc.observability.agent.dto.ResBody;
import mcmp.mc.observability.agent.model.HostInfo;
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

    private class RestHostList extends ResBody<PageableResBody<List<HostInfo>>> {}

    @GetMapping("")
    public ResBody list(PageableReqBody<HostInfo> req) {
        if( req.getData() == null ) req.setData(new HostInfo());

        ResBody res = new RestHostList();
        res.setData(hostService.getList(req));
        return res;
    }

    @PutMapping("")
    public ResBody update(@RequestBody HostInfo hostInfo) {
        ResBody res = hostService.updateHost(hostInfo);
        return res;
    }

    @GetMapping("/{hostSeq}")
    public ResBody detail(@PathVariable("hostSeq") Long seq) {
        return hostService.getDetail(new ResBody<HostInfo>(), seq);
    }

    @PutMapping("/{hostSeq}/turnMonitoringYn")
    public ResBody turnMonitoringYn(@PathVariable("hostSeq") Long seq) {
        return hostService.turnMonitoringYn(seq);
    }

    @PutMapping("/{hostSeq}/synchronize")
    public ResBody synchronizeAll(@PathVariable("hostSeq") Long hostSeq) {
        return hostService.synchronizeAll(hostSeq);
    }
}
