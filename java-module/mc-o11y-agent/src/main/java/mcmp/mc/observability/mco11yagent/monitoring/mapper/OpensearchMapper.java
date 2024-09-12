package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.mapper.handler.OpensearchInfoHandler;
import mcmp.mc.observability.mco11yagent.monitoring.model.OpensearchInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OpensearchMapper {

    OpensearchInfo getOpensearchInfo(Long seq);

    List<OpensearchInfo> getOpensearchInfoList();

    void getOpensearchInfoMap(OpensearchInfoHandler handler);

    default Map<Long, OpensearchInfo> getOpensearchInfoMap() {
        OpensearchInfoHandler handler = new OpensearchInfoHandler();
        this.getOpensearchInfoMap(handler);
        return handler.getResult();
    }

    void insertOpensearchInfoList(List<OpensearchInfo> list);

    void deleteOpensearchInfoList(List<Long> list);
}
