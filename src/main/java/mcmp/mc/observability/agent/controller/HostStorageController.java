package mcmp.mc.observability.agent.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.dto.PageableReqBody;
import mcmp.mc.observability.agent.dto.PageableResBody;
import mcmp.mc.observability.agent.dto.ResBody;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.service.HostStorageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.PREFIX_V1 + "/host/{hostSeq}/storage")
@RequiredArgsConstructor
public class HostStorageController {

    private final HostStorageService hostStorageService;

    @GetMapping("")
    public ResBody list(@PathVariable("hostSeq") Long hostSeq, PageableReqBody<HostStorageInfo> req) {
        if( req.getData() == null ) req.setData(new HostStorageInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody res = new ResBody<PageableReqBody<HostStorageInfo>>();
        res.setData(hostStorageService.getList(req));
        return res;
    }

    @GetMapping("/{storageSeq}")
    public ResBody detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {

        ResBody res = new ResBody();
        res.setData(hostStorageService.getStorageDetail(hostSeq, seq));
        return res;
    }

    @PostMapping("")
    public ResBody create(@PathVariable("hostSeq") Long hostSeq, @RequestBody List<HostStorageInfo> list) {
        return hostStorageService.insertStorage(hostSeq, list);
    }

    @DeleteMapping("/{storageSeq}")
    public ResBody delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.deleteStorage(hostSeq, seq);
    }

    @PutMapping("/{storageSeq}/turnMonitoringYn")
    public ResBody turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.turnMonitoringYn(hostSeq, seq);
    }
}
