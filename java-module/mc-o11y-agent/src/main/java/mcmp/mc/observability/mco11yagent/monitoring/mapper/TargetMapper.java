package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.model.TargetInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TargetMapper {
    List<TargetInfo> getList();
    TargetInfo getTarget(@Param("nsId") String nsId, @Param("mciId") String mciId, @Param("id") String targetId);
    int insert(TargetInfo targetInfo);
    int update(TargetInfo targetInfo);
    int updateState(@Param("nsId") String nsId, @Param("mciId") String mciId, @Param("id") String targetId, @Param("state") String state);
    int delete(TargetInfo targetInfo);
}
