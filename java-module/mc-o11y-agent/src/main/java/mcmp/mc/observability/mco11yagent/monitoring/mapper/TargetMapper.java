package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.model.TargetInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TargetMapper {
    List<TargetInfo> getList();
    int insert(TargetInfo targetInfo);
    int update(TargetInfo targetInfo);
    int updateState(@Param("nsId")String nsId, @Param("id") String targetId, @Param("state") String state);
    int delete(TargetInfo targetInfo);
}
