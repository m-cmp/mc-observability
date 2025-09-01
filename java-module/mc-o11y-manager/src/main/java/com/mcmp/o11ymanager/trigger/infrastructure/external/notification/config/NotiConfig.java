package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.config;


import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.EMAIL;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.KAKAO;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SLACK;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SMS;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiSender;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiSender;
import java.time.Duration;
import java.util.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;

/**
 * Configuration class for notification services Sets up notification channels including email, SMS,
 * Slack, and Kakao.
 */
@Configuration
@ConfigurationProperties(prefix = "notification")
public class NotiConfig {

    /**
     * Creates mail properties bean for email notifications.
     *
     * @param templateEngine the Thymeleaf template engine for email templates
     * @return MailProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.mail")
    public MailProperties mailProperties(TemplateEngine templateEngine) {
        return new MailProperties(templateEngine);
    }

    /**
     * Creates SMS properties bean for SMS notifications.
     *
     * @return SmsProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.sms")
    public SmsProperties smsProperties() {
        return new SmsProperties();
    }

    /**
     * Creates Slack properties bean for Slack notifications.
     *
     * @return SlackProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.slack")
    public SlackProperties slackProperties() {
        return new SlackProperties();
    }

    /**
     * Creates Kakao properties bean for Kakao notifications.
     *
     * @return KakaoProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.kakao")
    public KakaoProperties kakaoProperties() {
        return new KakaoProperties();
    }

    /**
     * Creates a notification factory registry with all notification types.
     *
     * @param mailProperties mail configuration properties
     * @param smsProperties SMS configuration properties
     * @param kakaoProperties Kakao configuration properties
     * @return NotiFactory with all notification channel configurations
     */
    @Bean
    public NotiFactory notiRegistry(
            MailProperties mailProperties,
            SmsProperties smsProperties,
            KakaoProperties kakaoProperties) {
        return DefaultNotiFactory.newInstance()
                .put(EMAIL, mailProperties)
                .put(SMS, smsProperties)
                .put(SLACK, slackProperties())
                .put(KAKAO, kakaoProperties);
    }

    /**
     * Creates a notification sender manager with all notification channels.
     *
     * @param mailNotifier email notification sender
     * @param smsNotifier SMS notification sender
     * @param slackNotifier Slack notification sender
     * @param kakaoNotifier Kakao notification sender
     * @return NotiSender with all notification channel implementations
     */
    @Bean
    public NotiSender notiManager(
            MailNotifier mailNotifier,
            SmsNotifier smsNotifier,
            SlackNotifier slackNotifier,
            KakaoNotifier kakaoNotifier) {
        return DefaultNotiSender.newInstance()
                .put(EMAIL, mailNotifier)
                .put(SMS, smsNotifier)
                .put(SLACK, slackNotifier)
                .put(KAKAO, kakaoNotifier);
    }

    /**
     * Creates a JavaMailSender bean for sending emails.
     *
     * @param mailProperties mail configuration properties
     * @return JavaMailSender configured for SMTP
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.mail")
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());
        mailSender.setProtocol("smtp");
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", mailProperties.isSmtpAuth());
        props.put("mail.smtp.starttls.enable", mailProperties.isTlsEnable());
        props.put("mail.smtp.starttls.required", mailProperties.isTlsRequired());
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Creates a RestClient bean with timeout configurations.
     *
     * @param builder the RestClient builder
     * @return RestClient configured with connection and read timeouts
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder.requestFactory(requestFactory).build();
    }
}
