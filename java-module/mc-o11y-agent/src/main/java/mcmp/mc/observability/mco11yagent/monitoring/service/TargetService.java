package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.TargetMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.TargetInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetMapper targetMapper;

    public ResBody<List<TargetInfo>> getList() {
        ResBody<List<TargetInfo>> resBody = new ResBody<>();
        resBody.setData(targetMapper.getList());
        return resBody;
    }

    public ResBody insert(String nsId, String targetId) {
        TargetInfo targetInfo = new TargetInfo();
        targetInfo.setNsId(nsId);
        targetInfo.setId(targetId);
        if( targetMapper.insert(targetInfo) > 0 ) {
            return ResBody.builder().code(ResultCode.SUCCESS).build();
        }
        else {
            return ResBody.builder().code(ResultCode.FAILED).build();
        }
    }

    public ResBody update(String nsId, String targetId, TargetInfo targetInfo) {
        targetInfo.setNsId(nsId);
        targetInfo.setId(targetId);
        if( targetMapper.update(targetInfo) > 0 ) {
            return ResBody.builder().code(ResultCode.SUCCESS).build();
        }
        else {
            return ResBody.builder().code(ResultCode.DATABASE_ERROR).build();
        }
    }

    public ResBody delete(String nsId, String targetId) {
        TargetInfo targetInfo = new TargetInfo();
        targetInfo.setNsId(nsId);
        targetInfo.setId(targetId);
        if( targetMapper.delete(targetInfo) > 0 ) {
            return ResBody.builder().code(ResultCode.SUCCESS).build();
        }
        else {
            return ResBody.builder().code(ResultCode.DATABASE_ERROR).build();
        }
    }
}
