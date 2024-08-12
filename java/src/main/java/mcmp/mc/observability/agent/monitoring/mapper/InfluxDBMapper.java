package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.monitoring.mapper.handler.InfluxDBInfoHandler;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface InfluxDBMapper {

    InfluxDBInfo getInfluxDBInfo(Long seq);

    List<InfluxDBInfo> getInfluxDBInfoList();

    void getInfluxDBInfoMap(InfluxDBInfoHandler handler);

    default Map<Long, InfluxDBInfo> getInfluxDBInfoMap() {
        InfluxDBInfoHandler handler = new InfluxDBInfoHandler();
        this.getInfluxDBInfoMap(handler);
        return handler.getResult();
    }

    void insertInfluxDBInfoList(List<InfluxDBInfo> list);

    void deleteInfluxDBInfoList(List<Long> list);
}
