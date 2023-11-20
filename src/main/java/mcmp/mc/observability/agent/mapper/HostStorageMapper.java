package mcmp.mc.observability.agent.mapper;

import mcmp.mc.observability.agent.dto.PageableReqBody;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HostStorageMapper {
    Long getListCount(PageableReqBody reqBody);
    List<HostStorageInfo> getList(PageableReqBody reqBody);
    HostStorageInfo getStorageDetail(Long hostSeq, Long seq);
    List<HostStorageInfo> getHostStorageList(Map<String, Object> params);

    Integer insertStorage(List<HostStorageInfo> list);
    Integer deleteStorage(Long hostSeq, Long seq);

    Integer updateStorageConf(Long seq);
    Integer deleteStorageRow(Long seq);

    Integer turnMonitoringYn(Long hostSeq, Long seq);
}