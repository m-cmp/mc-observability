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
import mcmp.mc.observability.agent.trigger.model.*;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerEmailUserCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.mail.internet.MimeMessage;
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


    public void sendEventAlert(TriggerAlertInfo alertInfo) {
        sendSlack(alertInfo);
        sendEmail(alertInfo);
    }

    public void sendSlack(TriggerAlertInfo alertInfo) {
        List<TriggerSlackUserInfo> slackUserList = slackMapper.getSlackUserListByPolicySeq(alertInfo.getPolicySeq());
        if (CollectionUtils.isEmpty(slackUserList))
            return;

        String message = alertInfo.getAlertMessage();
        for (TriggerSlackUserInfo slackUser : slackUserList) {
            try {
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(slackUser.getChannel())
                        .text(message)
                        .build();

                Slack slack = Slack.getInstance();
                ChatPostMessageResponse response = slack.methods(slackUser.getToken()).chatPostMessage(request);
                if (!response.isOk()) {
                    switch (response.getError()) {
                        case "invalid_auth":
                            log.error("Invalid authentication credentials for Slack.");
                        case "channel_not_found":
                            log.error("The specified channel was not found.");
                        default:
                            log.error("An error occurred with Slack API: {}", response.getError());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendEmail(TriggerAlertInfo alertInfo) {
        try {
            List<TriggerEmailUserInfo> emailUserInfoList = emailMapper.getEmailUserListByPolicySeq(alertInfo.getPolicySeq());
            if(CollectionUtils.isEmpty(emailUserInfoList))
                return;

            JavaMailSender emailSender = mailConfig.getJavaMailSender();
            MimeMessage mimeMessage = emailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("policyName", alertInfo.getPolicyName());
            context.setVariable("targetId", alertInfo.getTargetId());
            context.setVariable("nsId", alertInfo.getNsId());
            context.setVariable("targetName", alertInfo.getTargetName());
            context.setVariable("metric", alertInfo.getMetric());
            context.setVariable("level", alertInfo.getLevel());
            context.setVariable("threshold", alertInfo.getThreshold());
            context.setVariable("occurTime", alertInfo.getOccurTime());
            String html = templateEngine.process("TriggerAlertTemplate.html", context);

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            mimeMessageHelper.setSubject("[Alert] Trigger event occurred");
            mimeMessageHelper.setText(html, true);

            ClassPathResource resource = new ClassPathResource("static/images/mcmp-logo.png");
            mimeMessageHelper.addInline("logo_png", resource.getFile());

            for (TriggerEmailUserInfo emailUserInfo : emailUserInfoList) {
                mimeMessageHelper.setTo(emailUserInfo.getEmail());
                emailSender.send(mimeMessage);
            }

        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
