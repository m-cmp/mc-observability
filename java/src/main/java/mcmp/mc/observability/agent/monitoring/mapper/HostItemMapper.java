package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.monitoring.model.HostItemInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HostItemMapper {

    Long getListCount(PageableReqBody reqBody);
    List<HostItemInfo> getList(PageableReqBody reqBody);
    List<HostItemInfo> getHostItemList(Map<String, Object> params);
    HostItemInfo getDetail(Map<String, Long> params);
    int insertItem(HostItemInfo info);
    int updateItem(HostItemInfo info);
    int updateItemConf(Long seq);
    void deleteItem(Map<String, Object> params);
    void deleteItemRow(Long seq);
    void turnMonitoringYn(Long hostSeq, Long seq);
    void syncHost(Long hostSeq);
}