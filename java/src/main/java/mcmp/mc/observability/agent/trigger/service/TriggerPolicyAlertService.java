package mcmp.mc.observability.agent.trigger.service;

import com.slack.api.Slack;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.trigger.config.MailConfig;
import mcmp.mc.observability.agent.trigger.mapper.TriggerAlertEmailMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerAlertSlackMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.model.TriggerEmailUserInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerSlackUserInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerEmailUserCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.mail.internet.MimeMessage;
import javax.naming.AuthenticationException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPolicyAlertService {

    private final TriggerPolicyMapper policyMapper;
    private final TriggerAlertSlackMapper slackMapper;
    private final TriggerAlertEmailMapper emailMapper;
    private final MailConfig mailConfig;
    private final SpringTemplateEngine templateEngine;

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

    public List<TriggerEmailUserInfo> getEmailUserList(Long policySeq) {
        TriggerPolicyInfo policyInfo = policyMapper.getDetail(policySeq);
        if(policyInfo == null)
            throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "Trigger Policy is not exist. PolicySeq : {}", policySeq);

        return emailMapper.getEmailUserListByPolicySeq(policySeq);
    }

    public ResBody<Void> createEmailUser(TriggerEmailUserCreateDto dto) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo policyInfo = policyMapper.getDetail(dto.getPolicySeq());
        if(policyInfo == null)
            throw new ResultCodeException(ResultCode.NOT_FOUND_DATA, "Trigger Policy is not exist. PolicySeq : {}", dto.getPolicySeq());

        TriggerEmailUserInfo info = new TriggerEmailUserInfo();
        info.setCreatDto(dto);
        try {
            int result = emailMapper.createEmailUser(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Trigger Email User insert error QueryResult={}", result);
            }
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> deleteEmailUser(Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if (seq <= 0)
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Slack User Sequence Error");
            emailMapper.deleteEmailUser(seq);
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
