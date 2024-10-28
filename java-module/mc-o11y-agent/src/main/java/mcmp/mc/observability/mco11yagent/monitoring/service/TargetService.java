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

    private final InfluxDBService influxDBService;
    private final OpenSearchService openSearchService;

    private void updateLists() {
        influxDBService.getList();
        openSearchService.getList();
    }

    public ResBody<List<TargetInfo>> getList() {
        ResBody<List<TargetInfo>> resBody = new ResBody<>();
        resBody.setData(targetMapper.getList());
        return resBody;
    }

    public ResBody<List<TargetInfo>> getList(String nsId, String mciId) {
        ResBody<List<TargetInfo>> resBody = new ResBody<>();
        resBody.setData(targetMapper.getListNSMCI(nsId, mciId));
        return resBody;
    }

    public ResBody<TargetInfo> getTarget(String nsId, String mciId, String targetId) {
        ResBody<TargetInfo> resBody = new ResBody<>();
        resBody.setData(targetMapper.getTarget(nsId, mciId, targetId));
        return resBody;
    }

    public ResBody insert(String nsId, String mciId, String targetId, TargetInfo targetInfo) {
        targetInfo.setNsId(nsId);
        targetInfo.setMciId(mciId);
        targetInfo.setId(targetId);

        if( targetMapper.insert(targetInfo) > 0 ) {
            updateLists();

            return new ResBody<>(ResultCode.SUCCESS);
        } else {
            return new ResBody<>(ResultCode.FAILED);
        }
    }

    public ResBody update(String nsId, String mciId, String targetId, TargetInfo targetInfo) {
        targetInfo.setNsId(nsId);
        targetInfo.setMciId(mciId);
        targetInfo.setId(targetId);

        if( targetMapper.update(targetInfo) > 0 ) {
            updateLists();

            return new ResBody<>(ResultCode.SUCCESS);
        }
        else {
            return new ResBody<>(ResultCode.DATABASE_ERROR);
        }
    }

    public ResBody delete(String nsId, String mciId, String targetId) {
        TargetInfo targetInfo = new TargetInfo();
        targetInfo.setNsId(nsId);
        targetInfo.setMciId(mciId);
        targetInfo.setId(targetId);

        if( targetMapper.delete(targetInfo) > 0 ) {
            updateLists();

            return new ResBody<>(ResultCode.SUCCESS);
        }
        else {
            return new ResBody<>(ResultCode.DATABASE_ERROR);
        }
    }
}
