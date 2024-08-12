package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.annotation.Base64Decode;
import mcmp.mc.observability.agent.common.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.HostInfluxDBInfo;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.HostStorageCreateDTO;
import mcmp.mc.observability.agent.monitoring.model.dto.HostStorageUpdateDTO;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.HostStorageService;
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
@RequestMapping(Constants.MONITORING_URI + "/host/{hostSeq}/storage")
@RequiredArgsConstructor
public class HostStorageController {

    private final HostStorageService hostStorageService;

    @ApiOperation(value = "Get Host storage all list")
    @Base64Encode
    @GetMapping("")
    public ResBody<PageableResBody<HostStorageInfo>> list(@PathVariable("hostSeq") Long hostSeq, @ApiIgnore PageableReqBody<HostStorageInfo> req) {
        if( req.getData() == null ) req.setData(new HostStorageInfo());
        req.getData().setHostSeq(hostSeq);
        ResBody<PageableResBody<HostStorageInfo>> res = new ResBody<>();
        res.setData(hostStorageService.getList(req));
        return res;
    }

    @ApiOperation(value = "", hidden = true)
    @Base64Encode
    @GetMapping("/{storageSeq}")
    public ResBody<HostStorageInfo> detail(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {

        ResBody<HostStorageInfo> res = new ResBody<>();
        res.setData(hostStorageService.getStorageDetail(hostSeq, seq));
        return res;
    }

    @ApiOperation(value = "Create request Host storage")
    @Base64Decode(HostStorageCreateDTO.class)
    @PostMapping("")
    public ResBody<Void> create(@PathVariable("hostSeq") Long hostSeq, @RequestBody HostStorageCreateDTO info) {
        info.setHostSeq(hostSeq);
        return hostStorageService.createStorage(info);
    }

    @ApiOperation(value = "Update request Host storage")
    @Base64Decode(HostStorageUpdateDTO.class)
    @PutMapping("/{storageSeq}")
    public ResBody<Void> update(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq, @RequestBody HostStorageUpdateDTO info) {
        info.setSeq(seq);
        info.setHostSeq(hostSeq);
        return hostStorageService.updateStorage(info);
    }

    @ApiOperation(value = "Delete request Host storage")
    @DeleteMapping("/{storageSeq}")
    public ResBody<Void> delete(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.deleteStorage(hostSeq, seq);
    }

    @ApiOperation(value = "Update request Host storage monitoring state on/off")
    @PutMapping("/{storageSeq}/turnMonitoringYn")
    public ResBody<Void> turnMonitoringYn(@PathVariable("hostSeq") Long hostSeq, @PathVariable("storageSeq") Long seq) {
        return hostStorageService.turnMonitoringYn(hostSeq, seq);
    }

    @ApiOperation(value = "Get Host Storage Info")
    @GetMapping("/influxdb")
    public ResBody<HostInfluxDBInfo> getHostInfluxDbInfo(@PathVariable("hostSeq") Long hostSeq) {
        return hostStorageService.getHostInfluxDbInfo(new ResBody<>(), hostSeq);
    }

}
