package mcmp.mc.observability.agent.trigger.service;

import com.slack.api.Slack;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.trigger.mapper.TriggerAlertSlackMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerSlackUserInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPolicyAlertService {

    private final TriggerPolicyMapper policyMapper;
    private final TriggerAlertSlackMapper slackMapper;

    public List<TriggerSlackUserInfo> getSlackUserList(Long policySeq) {
        TriggerPolicyInfo policyInfo = policyMapper.getDetail(policySeq);
        if(policyInfo == null)
            throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "Trigger Policy is not exist. PolicySeq : {}", policySeq);

        return slackMapper.getSlackUserListByPolicySeq(policySeq);
    }

    public ResBody<Void> createSlackUser(TriggerSlackUserCreateDto dto) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo policyInfo = policyMapper.getDetail(dto.getPolicySeq());
        if(policyInfo == null)
            throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "Trigger Policy is not exist. PolicySeq : {}", dto.getPolicySeq());

        TriggerSlackUserInfo info = new TriggerSlackUserInfo();
        info.setCreateDto(dto);
        try {
            int result = slackMapper.createSlackUser(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Trigger Slack User insert error QueryResult={}", result);
            }
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> deleteSlackUser(Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if (seq <= 0)
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Slack User Sequence Error");
            slackMapper.deleteSlackUser(seq);
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> sendSlack(Long seq, String message) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            TriggerSlackUserInfo slackUser = slackMapper.getSlackUser(seq);
            if (slackUser == null)
                throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "Trigger Policy Slack User Sequence Error");

            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(slackUser.getChannel())
                    .text(message)
                    .build();

            Slack slack = Slack.getInstance();
            ChatPostMessageResponse response = slack.methods(slackUser.getToken()).chatPostMessage(request);
            if (!response.isOk()) {
                switch (response.getError()) {
                    case "invalid_auth":
                        throw new AuthenticationException("Invalid authentication credentials for Slack.");
                    case "channel_not_found":
                        throw new Exception("The specified channel was not found.");
                    default:
                        throw new Exception("An error occurred with Slack API: " + response.getError());
                }
            }
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resBody;
    }
}
