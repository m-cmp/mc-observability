package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.discord;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.DISCORD;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent.AlertDetail;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Discord notification data class Represents a Discord notification message formatted as one rich
 * embed per severity level for Discord's webhook API.
 */
@Getter
public class DiscordNoti implements Noti {

    private static final NotificationType notiType = DISCORD;

    private static final int COLOR_INFO = 5793266;
    private static final int COLOR_WARNING = 16705372;
    private static final int COLOR_CRITICAL = 15548997;

    private static final int MAX_DESCRIPTION = 4096;
    private static final String FOOTER_TEXT = "MC-Observability Alert Engine";

    private List<String> recipients;
    private RequestBody body;

    /**
     * Creates a DiscordNoti instance from alert event and Discord properties.
     *
     * @param event the alert event information
     * @param discordProperties Discord configuration properties
     * @param recipients list of Discord webhook URL recipients
     * @return DiscordNoti instance ready to be sent
     */
    public static Noti from(
            AlertEvent event, DiscordProperties discordProperties, List<String> recipients) {
        DiscordNoti notification = new DiscordNoti();
        notification.recipients = recipients;
        notification.body = buildBody(discordProperties, buildEmbeds(event));
        return notification;
    }

    public static DiscordNoti direct(
            DiscordProperties props, List<String> recipients, String title, String message) {
        DiscordNoti notification = new DiscordNoti();
        notification.recipients = recipients;
        notification.body = buildBody(props, List.of(buildDirectEmbed(title, message)));
        return notification;
    }

    /**
     * Builds the webhook request body with bot display settings and embeds.
     *
     * @param props Discord configuration properties
     * @param embeds the embeds to include in the message
     * @return RequestBody ready for serialization
     */
    private static RequestBody buildBody(DiscordProperties props, List<Embed> embeds) {
        RequestBody requestBody = new RequestBody();
        requestBody.username = props.getUsername();
        String avatarUrl = props.getAvatarUrl();
        requestBody.avatarUrl = (avatarUrl != null && !avatarUrl.isBlank()) ? avatarUrl : null;
        requestBody.embeds = embeds;
        return requestBody;
    }

    private static List<Embed> buildEmbeds(AlertEvent event) {
        List<Embed> embeds = new ArrayList<>();

        if (!event.getInfoAlerts().isEmpty()) {
            embeds.add(buildEmbed(event.getTitle(), "INFO", event.getInfoAlerts(), COLOR_INFO));
        }
        if (!event.getWarningAlerts().isEmpty()) {
            embeds.add(
                    buildEmbed(
                            event.getTitle(), "WARNING", event.getWarningAlerts(), COLOR_WARNING));
        }
        if (!event.getCriticalAlerts().isEmpty()) {
            embeds.add(
                    buildEmbed(
                            event.getTitle(),
                            "CRITICAL",
                            event.getCriticalAlerts(),
                            COLOR_CRITICAL));
        }

        return embeds;
    }

    private static Embed buildEmbed(
            String eventTitle, String severity, List<AlertDetail> alerts, int color) {
        Embed embed = new Embed();
        embed.title = "[M-CMP] " + severity + " [≥ " + alerts.get(0).getThreshold() + "%]";
        embed.color = color;
        embed.description =
                truncateDescription("> title: " + eventTitle + "\n" + buildTableMessage(alerts));
        embed.footer = new Footer(FOOTER_TEXT);
        embed.timestamp = Instant.now().toString();
        return embed;
    }

    private static Embed buildDirectEmbed(String title, String message) {
        Embed embed = new Embed();
        embed.title = "[M-CMP] " + title;
        embed.color = COLOR_INFO;
        embed.description = truncateDescription(message);
        embed.footer = new Footer(FOOTER_TEXT);
        embed.timestamp = Instant.now().toString();
        return embed;
    }

    private static String buildTableMessage(List<AlertDetail> alerts) {
        if (alerts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append(
                String.format(
                        "%-15s %-15s %-15s %-15s %s\n",
                        "Namespace ID", "Infra ID", "Node ID", "Metric", "Usage"));
        sb.append("─".repeat(90)).append("\n");

        for (AlertDetail alert : alerts) {
            sb.append(
                    String.format(
                            "%-15s %-15s %-15s %-15s %s%%\n",
                            truncateString(alert.getNamespaceId(), 14),
                            truncateString(alert.getInfraId(), 14),
                            truncateString(alert.getNodeId(), 14),
                            truncateString(alert.getResourceType(), 14),
                            alert.getResourceUsage()));
        }

        sb.append("```");
        return sb.toString();
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength - 1) + "…" : str;
    }

    private static String truncateDescription(String description) {
        if (description.length() <= MAX_DESCRIPTION) {
            return description;
        }
        String suffix = "\n…(truncated)\n```";
        return description.substring(0, MAX_DESCRIPTION - suffix.length()) + suffix;
    }

    @Override
    public NotificationType getNotificationType() {
        return notiType;
    }

    @Getter
    public static class RequestBody {

        private String username;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        private List<Embed> embeds;
    }

    @Getter
    public static class Embed {

        private String title;
        private String description;
        private Integer color;
        private Footer footer;
        private String timestamp;
    }

    @Getter
    public static class Footer {

        private final String text;

        public Footer(String text) {
            this.text = text;
        }
    }
}
