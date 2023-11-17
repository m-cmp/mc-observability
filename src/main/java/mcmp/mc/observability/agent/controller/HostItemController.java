package mcmp.mc.observability.agent.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.dto.PageableReqBody;
import mcmp.mc.observability.agent.dto.ResBody;
import mcmp.mc.observability.agent.model.HostItemInfo;
import mcmp.mc.observability.agent.service.HostItemService;
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
@RequestMapping(Constants.PREFIX_V1 + "/host/{hostSeq}/item")
@RequiredArgsConstructor
public class HostItemController {

    private final HostItemService hostItemService;

    @GetMapping("")
    public ResBody list(@PathVariable("hostSeq") Long hostSeq, PageableReqBody<HostItemInfo> req) {
        req.setData(new HostItemInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody res = new ResBody<List<HostItemInfo>>();
        res.setData(hostItemService.getList(req));
        return res;
    }

    @PostMapping("")
    public ResBody create(@PathVariable("hostSeq") Long hostSeq, @RequestBody HostItemInfo info) {
        info.setHostSeq(hostSeq);
        return hostItemService.insertItem(info);
    }

    @PutMapping("/{itemSeq}")
    public ResBody update(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq, @RequestBody HostItemInfo info) {
        info.setHostSeq(hostSeq);
        info.setSeq(seq);
        ResBody res = hostItemService.updateItem(info);
        return res;
    }

    @DeleteMapping("/{itemSeq}")
    public ResBody delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.deleteItem(hostSeq, seq);
    }

    @GetMapping("/{itemSeq}")
    public ResBody detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.getDetail(new ResBody<HostItemInfo>(), hostSeq,  seq);
    }

    @PutMapping("/{itemSeq}/turnMonitoringYn")
    public ResBody turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.turnMonitoringYn(hostSeq, seq);
    }

}