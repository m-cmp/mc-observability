package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.model.MiningDBInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.MiningDBSetDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MiningDBMapper {

    MiningDBInfo getDetail();

    int updateMiningDB(MiningDBSetDTO info);
}
