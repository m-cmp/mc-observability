package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HostStorageMapper {
    Long getListCount(PageableReqBody reqBody);
    List<HostStorageInfo> getList(PageableReqBody reqBody);
    HostStorageInfo getStorageDetail(Long hostSeq, Long seq);
    List<HostStorageInfo> getHostStorageList(Map<String, Object> params);
    List<HostStorageInfo> getHostStorageList();

    Integer createStorage(HostStorageInfo info);
    Integer updateStorage(HostStorageInfo info);
    Integer deleteStorage(Long hostSeq, Long seq);

    Integer updateStorageConf(Long seq);
    Integer deleteStorageRow(Long seq);

    Integer turnMonitoringYn(Long hostSeq, Long seq);
    void syncHost(Long hostSeq);
}