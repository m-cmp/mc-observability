package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.EMAIL;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.thymeleaf.TemplateEngine;

/**
 * Configuration properties for email notifications Contains SMTP server settings and template
 * engine for email generation.
 */
@Getter
@Setter
public class MailProperties implements NotiProperty {
    private final NotificationType type = EMAIL;
    private final TemplateEngine templateEngine;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean smtpAuth;
    private boolean tlsRequired;
    private boolean tlsEnable;

    /**
     * Constructor for MailProperties.
     *
     * @param templateEngine Thymeleaf template engine for email template processing
     */
    public MailProperties(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
}
