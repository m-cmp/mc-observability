package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.loader.PluginLoader;
import mcmp.mc.observability.agent.model.dto.HostStorageCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostStorageUpdateDTO;
import mcmp.mc.observability.agent.model.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.dto.PageableResBody;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.enums.ResultCode;
import mcmp.mc.observability.agent.exception.ResultCodeException;
import mcmp.mc.observability.agent.mapper.HostStorageMapper;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostStorageService {

    private final HostStorageMapper mapper;
    private final PluginLoader pluginLoader;

    public PageableResBody<HostStorageInfo> getList(PageableReqBody<HostStorageInfo> reqBody) {

        PageableResBody<HostStorageInfo> result = new PageableResBody<>();
        result.setRecords(mapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<HostStorageInfo> list = mapper.getList(reqBody);
            if( list == null ) list = new ArrayList<>();
            result.setRows(list);
        }

        return result;
    }

    public List<HostStorageInfo> getList(Map<String, Object> params) {
        return mapper.getHostStorageList(params);
    }

    public HostStorageInfo getStorageDetail(Long hostSeq, Long seq) {
        return mapper.getStorageDetail(hostSeq, seq);
    }

    public ResBody<Void> createStorage(HostStorageCreateDTO dto) {
        ResBody<Void> resBody = new ResBody<>();
        HostStorageInfo info = new HostStorageInfo();
        info.setCreateDto(dto, pluginLoader.getPluginDefInfo(dto.getPluginSeq()));

        try {
            int result = mapper.createStorage(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Host Storage insert error QueryResult={}", result);
            }
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> updateStorage(HostStorageUpdateDTO dto) {
        ResBody<Void> resBody = new ResBody<>();
        HostStorageInfo info = new HostStorageInfo();
        info.setUpdateDto(dto);

        try {
            if( info.getSeq() <= 0 ) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Storage Sequence Error");
            }

            int result = mapper.updateStorage(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Host Storage insert error QueryResult={}", result);
            }
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> deleteStorage(Long hostSeq, Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 )
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Storage Sequence Error");

            mapper.deleteStorage(hostSeq, seq);
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public void updateStorageConf(Long seq) {
        mapper.updateStorageConf(seq);
    }

    public void deleteStorageRow(Long seq) {
        mapper.deleteStorageRow(seq);
    }

    public ResBody<Void> turnMonitoringYn(Long hostSeq, Long seq) {

        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 )
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Storage Sequence Error");

            mapper.turnMonitoringYn(hostSeq, seq);
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
