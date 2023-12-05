package mcmp.mc.observability.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.model.dto.HostStorageCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostStorageUpdateDTO;
import mcmp.mc.observability.agent.model.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.dto.PageableResBody;
import mcmp.mc.observability.agent.model.dto.ResBody;
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

    @Operation(summary = "Get Host storage all list")
    @GetMapping("")
    public ResBody<PageableResBody<List<HostStorageInfo>>> list(@PathVariable("hostSeq") Long hostSeq, PageableReqBody<HostStorageInfo> req) {
        if( req.getData() == null ) req.setData(new HostStorageInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody<PageableResBody<List<HostStorageInfo>>> res = new ResBody<>();
        res.setData(hostStorageService.getList(req));
        return res;
    }

    @Operation(hidden = true)
    @GetMapping("/{storageSeq}")
    public ResBody<HostStorageInfo> detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {

        ResBody<HostStorageInfo> res = new ResBody<>();
        res.setData(hostStorageService.getStorageDetail(hostSeq, seq));
        return res;
    }

    @Operation(summary = "Create request Host storage")
    @PostMapping("")
    public ResBody<?> create(@PathVariable("hostSeq") Long hostSeq, @RequestBody HostStorageCreateDTO info) {
        info.setHostSeq(hostSeq);
        return hostStorageService.createStorage(info);
    }

    @Operation(summary = "Update request Host storage")
    @PutMapping("/{storageSeq}")
    public ResBody<?> update(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq, @RequestBody HostStorageUpdateDTO info) {
        info.setSeq(seq);
        info.setHostSeq(hostSeq);
        return hostStorageService.updateStorage(info);
    }

    @Operation(summary = "Delete request Host storage")
    @DeleteMapping("/{storageSeq}")
    public ResBody<?> delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.deleteStorage(hostSeq, seq);
    }

    @Operation(summary = "Update request Host storage monitoring state on/off")
    @PutMapping("/{storageSeq}/turnMonitoringYn")
    public ResBody<?> turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.turnMonitoringYn(hostSeq, seq);
    }
}
