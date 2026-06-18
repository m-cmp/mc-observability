package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.config;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.DISCORD;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.EMAIL;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.KAKAO;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SLACK;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SMS;
import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.TEAMS;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiSender;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.discord.DiscordNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.discord.DiscordProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoTemplateProvider;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.teams.TeamsNotifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.teams.TeamsProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiSender;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
     * Creates Discord properties bean for Discord notifications.
     *
     * @return DiscordProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.discord")
    public DiscordProperties discordProperties() {
        return new DiscordProperties();
    }

    /**
     * Creates Teams properties bean for Microsoft Teams notifications.
     *
     * @return TeamsProperties configured from application properties
     */
    @Bean
    @ConfigurationProperties(prefix = "notification.teams")
    public TeamsProperties teamsProperties() {
        return new TeamsProperties();
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
            KakaoProperties kakaoProperties,
            KakaoTemplateProvider kakaoTemplateProvider) {
        return DefaultNotiFactory.newInstance()
                .withKakaoTemplateProvider(kakaoTemplateProvider)
                .put(EMAIL, mailProperties)
                .put(SMS, smsProperties)
                .put(SLACK, slackProperties())
                .put(KAKAO, kakaoProperties)
                .put(DISCORD, discordProperties())
                .put(TEAMS, teamsProperties());
    }

    /**
     * Creates a notification sender manager with all notification channels.
     *
     * @param mailNotifier email notification sender
     * @param smsNotifier SMS notification sender
     * @param slackNotifier Slack notification sender
     * @param kakaoNotifier Kakao notification sender
     * @param discordNotifier Discord notification sender
     * @param teamsNotifier Teams notification sender
     * @return NotiSender with all notification channel implementations
     */
    @Bean
    public NotiSender notiManager(
            MailNotifier mailNotifier,
            SmsNotifier smsNotifier,
            SlackNotifier slackNotifier,
            KakaoNotifier kakaoNotifier,
            DiscordNotifier discordNotifier,
            TeamsNotifier teamsNotifier) {
        return DefaultNotiSender.newInstance()
                .put(EMAIL, mailNotifier)
                .put(SMS, smsNotifier)
                .put(SLACK, slackNotifier)
                .put(KAKAO, kakaoNotifier)
                .put(DISCORD, discordNotifier)
                .put(TEAMS, teamsNotifier);
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
        // Use the JDK HttpClient-based factory. Unlike SimpleClientHttpRequestFactory
        // (HttpURLConnection), it returns error responses (e.g. 401) instead of throwing
        // "HttpRetryException: cannot retry due to server authentication, in streaming
        // mode", so onStatus handlers can read and log the body.
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder.requestFactory(requestFactory).build();
    }
}
