package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Email notification sender implementation Handles sending email notifications using
 * JavaMailSender.
 */
@Slf4j
@Component
public class MailNotifier implements Notifier {

    private final JavaMailSender mailSender;

    /**
     * Constructor for MailNotifier.
     *
     * @param mailSender JavaMailSender for sending emails
     */
    public MailNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email notification.
     *
     * @param noti the notification to send (must be MailNoti)
     * @return result of the email delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        String recipients = "";

        try {
            if (!(noti instanceof MailNoti mailNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected MailNoti but got: " + noti.getClass().getSimpleName());
            }

            MimeMessage message = mailSender.createMimeMessage();
            String[] to = mailNoti.getTo().toArray(new String[0]);
            recipients = String.join(", ", to);
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailNoti.getFrom());
            helper.setTo(to);
            helper.setSubject(mailNoti.getSubject());
            helper.setText(mailNoti.getContent(), true);

            mailSender.send(message);
            return NotiResult.success(recipients);
        } catch (Exception e) {
            return NotiResult.fail(recipients, e);
        }
    }
}
