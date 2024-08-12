package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.monitoring.enums.TelegrafState;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HostMapper {

    Long getListCount(PageableReqBody reqBody);
    List<HostInfo> getList(PageableReqBody reqBody);
    HostInfo getDetail(Long seq);
    Long getHostSeq(String uuid);

    List<Map<String, Long>> getItemCount();
    List<Map<String, Long>> getItemCount(Long seq);
    List<Map<String, Long>> getStorageCount();
    List<Map<String, Long>> getStorageCount(Long seq);

    int insertHost(HostInfo hostInfo);
    int updateHost(HostInfo hostInfo);
    int turnMonitoringYn(Long seq);
    int updateTelegrafState(Long seq, TelegrafState telegrafState);
}