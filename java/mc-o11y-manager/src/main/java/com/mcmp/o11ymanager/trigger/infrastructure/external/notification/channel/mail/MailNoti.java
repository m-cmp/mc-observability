package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.EMAIL;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Email notification data class Contains all information needed to send an email notification.
 */
@Getter
@Builder
public class MailNoti implements Noti {

  private static final NotificationType notiType = EMAIL;
  private String subject;
  private String content;
  private String from;
  private List<String> to;
  private String contentType;

  /**
   * Creates a MailNoti instance from alert event and mail properties.
   *
   * @param event          the alert event information
   * @param mailProperties mail configuration properties
   * @param recipients     list of email recipients
   * @return MailNoti instance ready to be sent
   */
  public static Noti from(
      AlertEvent event, MailProperties mailProperties, List<String> recipients) {
    return MailNoti.builder()
        .subject(buildSubject(event))
        .content(buildContent(event, mailProperties.getTemplateEngine()))
        .from(mailProperties.getUsername())
        .to(recipients)
        .contentType(MimeTypeUtils.TEXT_HTML_VALUE)
        .build();
  }

  public static MailNoti direct(
      MailProperties mailProperties, List<String> recipients, String title, String message) {

    return MailNoti.builder()
        .subject(buildDirectSubject(title))
        .content(buildDirectContent(title, message))
        .from(mailProperties.getUsername())
        .to(recipients)
        .contentType(MimeTypeUtils.TEXT_HTML_VALUE)
        .build();
  }

  private static String buildDirectSubject(String title) {
    return "[M-CMP] " + title;
  }


  private static String buildDirectContent(String title, String message) {
    return """
        <html>
        <body style="font-family: Arial, sans-serif; line-height:1.6;">
            <p><strong>Title :</strong> %s</p>
            <p><strong>Message :</strong> <span style="white-space:pre-wrap;">%s</span></p>
        </body>
        </html>
        """.formatted(title, message);
  }


  /**
   * Builds email subject from alert event.
   *
   * @param event the alert event
   * @return formatted email subject
   */
  private static String buildSubject(AlertEvent event) {
    return String.format(
        "[M-CMP] %d alerts triggered in %s", event.getAlertsCount(), event.getTitle());
  }

  /**
   * Builds email content using Thymeleaf template.
   *
   * @param event          the alert event
   * @param templateEngine Thymeleaf template engine
   * @return HTML email content
   */
  private static String buildContent(AlertEvent event, TemplateEngine templateEngine) {
    Context context = new Context();
    context.setVariable("event", event);
    context.setVariable(
        "timestamp",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    return templateEngine.process("mail/alert-notification", context);
  }

  @Override
  public NotificationType getNotificationType() {
    return notiType;
  }
}
