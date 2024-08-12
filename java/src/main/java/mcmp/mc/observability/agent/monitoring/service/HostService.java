package mcmp.mc.observability.agent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import mcmp.mc.observability.agent.monitoring.enums.TelegrafState;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.monitoring.mapper.HostItemMapper;
import mcmp.mc.observability.agent.monitoring.mapper.HostMapper;
import mcmp.mc.observability.agent.monitoring.mapper.HostStorageMapper;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.HostUpdateDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostService {

    private final HostMapper hostMapper;
    private final HostItemMapper hostItemMapper;
    private final HostStorageMapper hostStorageMapper;

    public PageableResBody<HostInfo> getList(PageableReqBody<HostInfo> reqBody) {
        PageableResBody<HostInfo> result = new PageableResBody<>();
        result.setRecords(hostMapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<HostInfo> list = hostMapper.getList(reqBody);
            if (list != null && list.size() > 0) {
                Map<Long, Long> itemMap = this.getItemCount();
                Map<Long, Long> storageMap = this.getStorageCount();
                for (HostInfo hostInfo : list) {
                    hostInfo.mappingCount(itemMap, storageMap);
                }
            } else {
                list = new ArrayList<>();
            }
            result.setRows(list);
        }

        return result;
    }

    private Map<Long, Long> getItemCount() {
        return convertList(hostMapper.getItemCount());
    }
    private Map<Long, Long> getItemCount(Long seq) {
        return convertList(hostMapper.getItemCount(seq));
    }

    private Map<Long, Long> getStorageCount() {
        return convertList(hostMapper.getStorageCount());
    }
    private Map<Long, Long> getStorageCount(Long seq) {
        return convertList(hostMapper.getStorageCount(seq));
    }

    private Map<Long, Long> convertList(List<Map<String, Long>> list) {
        Map<Long, Long> result = new HashMap<>();
        for( Map<String, Long> row : list ) {
            result.put(row.get("seq"), (result.get(row.get("seq")) != null? result.get(row.get("seq")): 0) + row.get("count"));
        }
        return result;
    }

    public void insertHost(HostInfo hostInfo) {
        try {
            if (hostInfo.getUuid() == null || hostInfo.getUuid().isEmpty()) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Monitoring Host UUID is null/empty");
            }
            int result = hostMapper.insertHost(hostInfo);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Monitoring Host insert error QueryResult={}", result);
            }
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
        }
    }

    public ResBody<Void> updateHost(HostUpdateDTO dto) {
        HostInfo hostInfo = new HostInfo();
        hostInfo.setUpdateHostDTO(dto);
        return updateHost(hostInfo);
    }

    public ResBody<Void> updateHost(HostInfo hostInfo) {
        ResBody<Void> resBody = new ResBody<>();

        try {
            if( hostInfo.getSeq() != null && getDetail(hostInfo.getSeq()) == null ) {
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Target Host No such data from Database");
            }

            int result = hostMapper.updateHost(hostInfo);

            if( result <= 0 ) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Host Update None");
            }
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<HostInfo> getDetail(ResBody<HostInfo> resBody, Long seq) {
        HostInfo hostInfo = getDetail(seq);
        if( hostInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(hostInfo);
        return resBody;
    }

    public HostInfo getDetail(Long seq) {
        HostInfo info = hostMapper.getDetail(seq);
        if( info != null ) {
            Map<Long, Long> itemMap = this.getItemCount(seq);
            Map<Long, Long> storageMap = this.getStorageCount(seq);
            info.mappingCount(itemMap, storageMap);
        }

        return info;
    }

    public Long getHostSeq(String uuid) {
        return hostMapper.getHostSeq(uuid);
    }

    public ResBody<Void> turnMonitoringYn(Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 ) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Host Sequence Error");
            }
            hostMapper.turnMonitoringYn(seq);

        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> synchronizeAll(Long hostSeq) {
        ResBody<Void> resBody = new ResBody<>();

        hostMapper.updateHost(HostInfo.builder().seq(hostSeq).syncYN(StateYN.N).build());
        hostItemMapper.syncHost(hostSeq);
        hostStorageMapper.syncHost(hostSeq);

        return resBody;
    }

    public void updateTelegrafState(Long seq, TelegrafState telegrafState) {
        hostMapper.updateTelegrafState(seq, telegrafState);
    }
}
