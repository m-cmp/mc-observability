package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.mapper.handler.OpenSearchInfoHandler;
import mcmp.mc.observability.mco11yagent.monitoring.model.OpenSearchInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OpenSearchMapper {

    OpenSearchInfo getOpenSearchInfo(Long seq);

    List<OpenSearchInfo> getOpenSearchInfoList();

    void getOpenSearchInfoMap(OpenSearchInfoHandler handler);

    default Map<Long, OpenSearchInfo> getOpenSearchInfoMap() {
        OpenSearchInfoHandler handler = new OpenSearchInfoHandler();
        this.getOpenSearchInfoMap(handler);
        return handler.getResult();
    }

    void insertOpenSearchInfoList(List<OpenSearchInfo> list);

    void deleteOpenSearchInfoList(List<Long> list);
}
