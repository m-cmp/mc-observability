package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.monitoring.model.MiningDBInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.MiningDBSetDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MiningDBMapper {

    MiningDBInfo getDetail();

    int updateMiningDB(MiningDBSetDTO info);
}
