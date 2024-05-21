package mcmp.mc.observability.agent.mapper;

import mcmp.mc.observability.agent.model.MiningDBInfo;
import mcmp.mc.observability.agent.model.dto.MiningDBCreateDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MiningDBMapper {

    int insertMiningDB(MiningDBCreateDTO info);

    MiningDBInfo getDetail();
}
