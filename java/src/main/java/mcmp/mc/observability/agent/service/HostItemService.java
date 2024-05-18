package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.dto.PageableResBody;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.enums.ResultCode;
import mcmp.mc.observability.agent.exception.ResultCodeException;
import mcmp.mc.observability.agent.loader.PluginLoader;
import mcmp.mc.observability.agent.mapper.HostItemMapper;
import mcmp.mc.observability.agent.mapper.HostMapper;
import mcmp.mc.observability.agent.model.HostInfo;
import mcmp.mc.observability.agent.model.HostItemInfo;
import mcmp.mc.observability.agent.model.PluginDefInfo;
import mcmp.mc.observability.agent.model.dto.HostItemCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostItemUpdateDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostItemService {

    private final HostItemMapper hostItemMapper;
    private final HostMapper hostMapper;
    private final PluginLoader pluginLoader;

    public PageableResBody<List<HostItemInfo>> getList(PageableReqBody reqBody) {
        PageableResBody<List<HostItemInfo>> result = new PageableResBody<>();
        result.setRecords(hostItemMapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<HostItemInfo> itemList = hostItemMapper.getList(reqBody);
            result.setRows(itemList);
        }

        return result;
    }

    public List<HostItemInfo> getList(Map<String, Object> params) {
        return hostItemMapper.getHostItemList(params);
    }

    public ResBody<Void> insertItem(HostItemCreateDTO dto) {
        ResBody<Void> resBody = new ResBody<>();
        HostItemInfo info = new HostItemInfo();
        info.setCreateDto(dto);
        try {
            if (info.getName() == null || info.getName().isEmpty()) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Name is null/empty");
            }
            PluginDefInfo plugin = pluginLoader.getPluginDefInfo(info.getPluginSeq());
            if (info.getPluginSeq() <= 0 || plugin == null) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Plugin is null");
            }
            if (info.getHostSeq() <= 0) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item target Host Seq is null");
            }
            HostInfo detail = hostMapper.getDetail(info.getHostSeq());
            if( detail == null ) {
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Target Host No such data from Database");
            }
            info.setPluginName(plugin.getName());

            if( info.getIntervalSec() < Constants.INTERVAL_MIN) {
                info.setIntervalSec(Constants.INTERVAL_MIN);
            }
            int result = hostItemMapper.insertItem(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Host Item insert error QueryResult={}", result);
            }

        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> updateItem(HostItemUpdateDTO dto) {
        ResBody<Void> resBody = new ResBody<>();
        HostItemInfo info = new HostItemInfo();
        info.setUpdateDto(dto);
        try {
            if( info.getSeq() <= 0 ) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Sequence Error");
            }

            if( info.getIntervalSec() < Constants.INTERVAL_MIN) {
                info.setIntervalSec(Constants.INTERVAL_MIN);
            }

            HostItemInfo hostItemInfo = getDetail(info.getHostSeq(), info.getSeq());
            if( hostItemInfo == null ) {
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Target Host Item No such data from Database");
            }

            int result = hostItemMapper.updateItem(info);

            if( result <= 0 ) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Host Item Update None");
            }

        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public void updateItemConf(Long seq) {
        hostItemMapper.updateItemConf(seq);
    }

    public ResBody<Void> deleteItem(Long hostSeq, Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 )
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Sequence Error");

            HostItemInfo hostItemInfo = getDetail(hostSeq, seq);
            if(hostItemInfo == null)
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Host Item Sequence Error");

            deleteItem(Collections.singletonMap("seq", seq));

        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public void deleteItem(Map<String, Object> params) {
        hostItemMapper.deleteItem(params);
    }


    public void deleteItemRow(Long seq) {
        hostItemMapper.deleteItemRow(seq);
    }

    public ResBody<HostItemInfo> getDetail(ResBody<HostItemInfo> resBody, Long hostSeq, Long seq) {
        HostItemInfo hostItemInfo = getDetail(hostSeq, seq);
        if( hostItemInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(hostItemInfo);
        return resBody;
    }

    public HostItemInfo getDetail(Long hostSeq, Long seq) {
        Map<String, Long> params = new HashMap<>();
        params.put("hostSeq", hostSeq);
        params.put("seq", seq);
        return hostItemMapper.getDetail(params);
    }

    public ResBody<Void> turnMonitoringYn(Long hostSeq, Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if (seq <= 0) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Sequence Error");
            }

            HostItemInfo hostItemInfo = getDetail(hostSeq, seq);
            if(hostItemInfo == null)
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Item Sequence Error");

            hostItemMapper.turnMonitoringYn(hostSeq, seq);

        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
