package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SLACK;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent.AlertDetail;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;

/**
 * Slack notification data class Represents a Slack notification message with formatted alert data
 * for Slack's attachment format.
 */
@Getter
public class SlackNoti implements Noti {
    private static final NotificationType notiType = SLACK;
    private List<String> recipients;
    private RequestHeader header;
    private RequestBody body;

    /**
     * Creates a SlackNoti instance from alert event and Slack properties.
     *
     * @param event the alert event information
     * @param slackProperties Slack configuration properties
     * @param recipients list of Slack channel recipients
     * @return SlackNoti instance ready to be sent
     */
    public static Noti from(
            AlertEvent event, SlackProperties slackProperties, List<String> recipients) {
        SlackNoti notification = new SlackNoti();
        notification.recipients = recipients;
        notification.header = buildHeader(slackProperties);
        notification.body = buildBody(event);
        return notification;
    }

    /**
     * Builds request header for Slack API call.
     *
     * @param slackProperties Slack configuration properties
     * @return RequestHeader with API URL and authorization token
     */
    private static RequestHeader buildHeader(SlackProperties slackProperties) {
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.url = slackProperties.getApiUrl();
        requestHeader.authorization = slackProperties.getToken();
        return requestHeader;
    }

    /**
     * Builds request body with alert attachments for Slack message.
     *
     * @param event the alert event
     * @return RequestBody with formatted attachments
     */
    private static RequestBody buildBody(AlertEvent event) {
        RequestBody requestBody = new RequestBody();
        requestBody.attachments = buildAttachments(event);
        return requestBody;
    }

    private static List<Attachment> buildAttachments(AlertEvent event) {
        Attachment attachment = new Attachment();
        attachment.pretext += event.getAlertsCount() + " alerts triggered.";
        attachment.text = buildText(event);
        attachment.fields = buildFields(event);
        attachment.ts = String.valueOf(System.currentTimeMillis());
        return List.of(attachment);
    }

    private static String buildText(AlertEvent event) {
        return "> title: " + event.getTitle() + "\n";
    }

    private static List<Field> buildFields(AlertEvent event) {
        ArrayList<Field> fields = new ArrayList<>();

        if (!event.getInfoAlerts().isEmpty()) {
            Field infoField = new Field();
            infoField.title = "Info [≥ " + event.getInfoAlerts().get(0).getThreshold() + "%]";
            infoField.value = buildTableMessage(event.getInfoAlerts());
            fields.add(infoField);
        }

        if (!event.getWarningAlerts().isEmpty()) {
            Field warningField = new Field();
            warningField.title =
                    "Warning [≥" + event.getWarningAlerts().get(0).getThreshold() + "%]";
            warningField.value = buildTableMessage(event.getWarningAlerts());
            fields.add(warningField);
        }

        if (!event.getCriticalAlerts().isEmpty()) {
            Field criticalField = new Field();
            criticalField.title =
                    "Critical [≥" + event.getCriticalAlerts().get(0).getThreshold() + "%]";
            criticalField.value = buildTableMessage(event.getCriticalAlerts());
            fields.add(criticalField);
        }

        return fields;
    }

    private static String buildTableMessage(List<AlertDetail> alerts) {
        if (alerts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append(
                String.format(
                        "%-15s %-15s %-15s %s\n", "Namespace ID", "MCI ID", "VM ID", "Usage"));
        sb.append("─".repeat(70)).append("\n");

        for (AlertDetail alert : alerts) {
            sb.append(
                    String.format(
                            "%-15s %-15s %-15s %s%%\n",
                            truncateString(alert.getNamespaceId(), 14),
                            truncateString(alert.getMciId(), 14),
                            truncateString(alert.getTargetId(), 14),
                            alert.getResourceUsage()));
        }

        sb.append("```");
        return sb.toString();
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 1) + "…" : str;
    }

    @Override
    public NotificationType getNotificationType() {
        return notiType;
    }

    public void updateChannel(String channel) {
        this.body.channel = channel;
    }

    @Getter
    public static class RequestHeader {
        private static final String contentType = MimeTypeUtils.APPLICATION_JSON.toString();
        private String url;
        private String authorization;

        public String getContentType() {
            return contentType;
        }
    }

    @Getter
    public static class RequestBody {
        private String channel;
        private List<Attachment> attachments;
    }

    @Getter
    public static class Attachment {
        private final String color = "#3AA3E3";
        private final String fallback = "(Fallback) Failed to send message.";
        private String pretext = "[M-CMP] ";
        private final String title = "Trigger Detail";
        private String text;
        private List<Field> fields;
        private String ts;
    }

    @Getter
    public static class Field {
        private final boolean isShort = false;
        private String title;
        private String value;
    }
}
