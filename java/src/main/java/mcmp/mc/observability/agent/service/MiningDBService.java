package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.enums.ResultCode;
import mcmp.mc.observability.agent.exception.ResultCodeException;
import mcmp.mc.observability.agent.mapper.MiningDBMapper;
import mcmp.mc.observability.agent.model.MiningDBInfo;
import mcmp.mc.observability.agent.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.agent.model.dto.ResBody;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiningDBService {

    private final MiningDBMapper miningDBMapper;

    public ResBody detail(ResBody<MiningDBInfo> resBody) {
        MiningDBInfo miningDBInfo = miningDBMapper.getDetail();
        if( miningDBInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(miningDBInfo);
        return resBody;
    }

    public ResBody setMiningDB(MiningDBSetDTO info) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            MiningDBInfo miningDBInfo = miningDBMapper.getDetail();
            if(miningDBInfo != null)
                miningDBMapper.deleteMiningDB(miningDBInfo.getSeq());

            int result = miningDBMapper.insertMiningDB(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "MiningDB insert error QueryResult={}", result);
            }
        } catch (ResultCodeException e) {
            resBody.setCode(e.getResultCode());
        } catch (Exception e) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
        }
        return resBody;
    }
}
