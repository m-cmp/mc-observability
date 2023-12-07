package mcmp.mc.observability.agent.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.HostItemInfo;
import mcmp.mc.observability.agent.model.dto.HostItemCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostItemUpdateDTO;
import mcmp.mc.observability.agent.model.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.dto.PageableResBody;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.service.HostItemService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping(Constants.PREFIX_V1 + "/host/{hostSeq}/item")
@RequiredArgsConstructor
public class HostItemController {

    private final HostItemService hostItemService;

    @ApiOperation(value = "Get Host item all list")
    @GetMapping("")
    public ResBody<PageableResBody<List<HostItemInfo>>> list(@PathVariable("hostSeq") Long hostSeq, @ApiIgnore PageableReqBody<HostItemInfo> req) {
        req.setData(new HostItemInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody<PageableResBody<List<HostItemInfo>>> res = new ResBody<>();
        res.setData(hostItemService.getList(req));
        return res;
    }

    @ApiOperation(value = "Create request Host item")
    @PostMapping("")
    public ResBody<?> create(@PathVariable("hostSeq") Long hostSeq, @RequestBody HostItemCreateDTO info) {
        info.setHostSeq(hostSeq);
        return hostItemService.insertItem(info);
    }

    @ApiOperation(value = "Update request Host item")
    @PutMapping("/{itemSeq}")
    public ResBody<?> update(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq, @RequestBody HostItemUpdateDTO info) {
        info.setHostSeq(hostSeq);
        info.setSeq(seq);
        return hostItemService.updateItem(info);
    }

    @ApiOperation(value = "Delete request Host item")
    @DeleteMapping("/{itemSeq}")
    public ResBody<?> delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.deleteItem(hostSeq, seq);
    }

    @ApiOperation(value = "", hidden = true)
    @GetMapping("/{itemSeq}")
    public ResBody<HostItemInfo> detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.getDetail(new ResBody<>(), hostSeq,  seq);
    }

    @ApiOperation(value = "Update request Host item monitoring state on/off")
    @PutMapping("/{itemSeq}/turnMonitoringYn")
    public ResBody<?> turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("itemSeq") Long seq) {
        return hostItemService.turnMonitoringYn(hostSeq, seq);
    }

}