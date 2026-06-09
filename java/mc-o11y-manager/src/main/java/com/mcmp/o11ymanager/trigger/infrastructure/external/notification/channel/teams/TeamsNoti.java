package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.teams;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.TEAMS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent.AlertDetail;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Microsoft Teams notification data class Represents a Teams notification message formatted as a
 * single Adaptive Card with one container per severity level for the Workflows webhook API.
 */
@Getter
public class TeamsNoti implements Noti {

    private static final NotificationType notiType = TEAMS;

    private static final String STYLE_INFO = "accent";
    private static final String STYLE_WARNING = "warning";
    private static final String STYLE_CRITICAL = "attention";

    private static final String COLOR_INFO = "Accent";
    private static final String COLOR_WARNING = "Warning";
    private static final String COLOR_CRITICAL = "Attention";

    private static final int MAX_ALERTS_PER_SEVERITY = 30;
    private static final String TITLE_PREFIX = "[M-CMP] ";
    private static final String FOOTER_TEXT = "MC-Observability Alert Engine";

    private List<String> recipients;
    private Message body;

    /**
     * Creates a TeamsNoti instance from alert event and Teams properties.
     *
     * @param event the alert event information
     * @param teamsProperties Teams configuration properties
     * @param recipients list of Teams Workflows webhook URL recipients
     * @return TeamsNoti instance ready to be sent
     */
    public static Noti from(
            AlertEvent event, TeamsProperties teamsProperties, List<String> recipients) {
        TeamsNoti notification = new TeamsNoti();
        notification.recipients = recipients;
        notification.body = buildMessage(buildEventBody(event));
        return notification;
    }

    public static TeamsNoti direct(
            TeamsProperties props, List<String> recipients, String title, String message) {
        TeamsNoti notification = new TeamsNoti();
        notification.recipients = recipients;
        notification.body = buildMessage(buildDirectBody(title, message));
        return notification;
    }

    /**
     * Wraps the Adaptive Card body elements into the Teams message envelope.
     *
     * @param body the card body elements to include
     * @return Message ready for serialization
     */
    private static Message buildMessage(List<CardElement> body) {
        Card card = new Card();
        card.body = body;
        Attachment attachment = new Attachment();
        attachment.content = card;
        Message message = new Message();
        message.attachments = List.of(attachment);
        return message;
    }

    private static List<CardElement> buildEventBody(AlertEvent event) {
        List<CardElement> body = new ArrayList<>();
        body.add(header(TITLE_PREFIX + event.getTitle()));

        if (!event.getInfoAlerts().isEmpty()) {
            body.add(severityContainer("INFO", event.getInfoAlerts(), STYLE_INFO, COLOR_INFO));
        }
        if (!event.getWarningAlerts().isEmpty()) {
            body.add(
                    severityContainer(
                            "WARNING", event.getWarningAlerts(), STYLE_WARNING, COLOR_WARNING));
        }
        if (!event.getCriticalAlerts().isEmpty()) {
            body.add(
                    severityContainer(
                            "CRITICAL", event.getCriticalAlerts(), STYLE_CRITICAL, COLOR_CRITICAL));
        }

        body.add(footer());
        return body;
    }

    private static List<CardElement> buildDirectBody(String title, String message) {
        List<CardElement> body = new ArrayList<>();
        body.add(header(TITLE_PREFIX + title));

        Container container = new Container();
        container.style = STYLE_INFO;
        container.items = List.of(text(message));
        body.add(container);

        body.add(footer());
        return body;
    }

    private static Container severityContainer(
            String severity, List<AlertDetail> alerts, String style, String color) {
        Container container = new Container();
        container.style = style;

        List<CardElement> items = new ArrayList<>();
        items.add(severityHeader(severity + " [≥ " + alerts.get(0).getThreshold() + "%]", color));

        int limit = Math.min(alerts.size(), MAX_ALERTS_PER_SEVERITY);
        for (int i = 0; i < limit; i++) {
            items.add(alertFacts(alerts.get(i), i > 0));
        }
        if (alerts.size() > limit) {
            items.add(text("…and " + (alerts.size() - limit) + " more"));
        }

        container.items = items;
        return container;
    }

    private static FactSet alertFacts(AlertDetail alert, boolean separator) {
        FactSet factSet = new FactSet();
        factSet.separator = separator ? Boolean.TRUE : null;
        factSet.facts =
                List.of(
                        new Fact("Namespace ID", safe(alert.getNamespaceId())),
                        new Fact("Infra ID", safe(alert.getInfraId())),
                        new Fact("Node ID", safe(alert.getNodeId())),
                        new Fact("Metric", safe(alert.getResourceType())),
                        new Fact("Usage", alert.getResourceUsage() + "%"));
        return factSet;
    }

    private static TextBlock header(String title) {
        TextBlock textBlock = new TextBlock();
        textBlock.text = title;
        textBlock.weight = "Bolder";
        textBlock.size = "Large";
        return textBlock;
    }

    private static TextBlock severityHeader(String title, String color) {
        TextBlock textBlock = new TextBlock();
        textBlock.text = title;
        textBlock.weight = "Bolder";
        textBlock.color = color;
        return textBlock;
    }

    private static TextBlock text(String value) {
        TextBlock textBlock = new TextBlock();
        textBlock.text = value;
        return textBlock;
    }

    private static TextBlock footer() {
        TextBlock textBlock = new TextBlock();
        textBlock.text = FOOTER_TEXT;
        textBlock.size = "Small";
        textBlock.subtle = Boolean.TRUE;
        textBlock.separator = Boolean.TRUE;
        return textBlock;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public NotificationType getNotificationType() {
        return notiType;
    }

    public interface CardElement {}

    @Getter
    public static class Message {

        private final String type = "message";
        private List<Attachment> attachments;
    }

    @Getter
    public static class Attachment {

        private final String contentType = "application/vnd.microsoft.card.adaptive";
        private Card content;
    }

    @Getter
    public static class Card {

        @JsonProperty("$schema")
        private final String schema = "http://adaptivecards.io/schemas/adaptive-card.json";

        private final String type = "AdaptiveCard";
        private final String version = "1.4";
        private List<CardElement> body;
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Container implements CardElement {

        private final String type = "Container";
        private String style;
        private List<CardElement> items;
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextBlock implements CardElement {

        private final String type = "TextBlock";
        private String text;
        private String weight;
        private String size;
        private String color;
        private boolean wrap = true;

        @JsonProperty("isSubtle")
        private Boolean subtle;

        private Boolean separator;
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FactSet implements CardElement {

        private final String type = "FactSet";
        private Boolean separator;
        private List<Fact> facts;
    }

    @Getter
    public static class Fact {

        private final String title;
        private final String value;

        public Fact(String title, String value) {
            this.title = title;
            this.value = value;
        }
    }
}
