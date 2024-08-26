package mcmp.mc.observability.agent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.monitoring.mapper.MiningDBMapper;
import mcmp.mc.observability.agent.monitoring.model.MiningDBInfo;
import mcmp.mc.observability.agent.monitoring.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.agent.common.model.ResBody;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiningDBService {

    private final MiningDBMapper miningDBMapper;

    public ResBody<MiningDBInfo> detail() {
        ResBody<MiningDBInfo> res = new ResBody<>();
        try {
            res.setData(miningDBMapper.getDetail());
        } catch (TooManyResultsException e) {
            throw new ResultCodeException(ResultCode.DATABASE_ERROR, "Mining Database TooManyResultsException");
        }
        return res;
    }

    public ResBody<Void> updateMiningDB(MiningDBSetDTO info) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            MiningDBInfo miningDBInfo = miningDBMapper.getDetail();
            if(miningDBInfo == null)
                throw new ResultCodeException(ResultCode.DATABASE_ERROR, "No Mining DB info");

            info.setOldUrl(miningDBInfo.getUrl());
            info.setOldDatabase(miningDBInfo.getDatabase());
            info.setOldRetentionPolicy(miningDBInfo.getRetentionPolicy());
            info.setOldUsername(miningDBInfo.getUsername());
            info.setOldPassword(miningDBInfo.getPassword());

            int result = miningDBMapper.updateMiningDB(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "MiningDB update error QueryResult={}", result);
            }
        } catch (TooManyResultsException e) {
            throw new ResultCodeException(ResultCode.DATABASE_ERROR, "Mining Database TooManyResultsException");
        } catch (Exception e) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, e.getMessage());
        }
        return resBody;
    }
}
