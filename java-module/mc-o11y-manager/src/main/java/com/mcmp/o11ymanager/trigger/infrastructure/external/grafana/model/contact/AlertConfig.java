package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Alert configuration model for Grafana notification contact points Manages receiver configurations
 * for alert notifications including MQTT settings. Used to configure and test alert notification
 * channels in Grafana.
 */
@Builder
@Getter
public class AlertConfig {
    private List<Receiver> receivers;

    /**
     * Creates test alert configuration for MCMP receiver Filters for 'mcmp' receiver configuration
     * and sets test message parameters.
     *
     * @param configs List of available receiver configurations
     * @return AlertConfig configured for testing with test message
     */
    public static AlertConfig createAlertTestConfig(List<GrafanaManagedReceiverConfig> configs) {
        GrafanaManagedReceiverConfig config =
                configs.stream()
                        .filter(receiver -> receiver.getName().equals("mcmp"))
                        .findFirst()
                        .orElse(null);
        config.settings.message = "This is test message.";
        config.settings.messageFormat = "json";
        Receiver receiver =
                Receiver.builder()
                        .name("mcmp")
                        .grafanaManagedReceiverConfigs(List.of(config))
                        .build();
        return AlertConfig.builder().receivers(List.of(receiver)).build();
    }

    /**
     * Receiver nested class for notification receiver configuration Contains receiver name and
     * associated Grafana managed receiver configurations.
     */
    @Getter
    @Builder
    public static class Receiver {
        private String name;

        @JsonProperty("grafana_managed_receiver_configs")
        private List<Object> grafanaManagedReceiverConfigs;
    }

    /**
     * Grafana managed receiver configuration for notification channel setup Contains receiver type,
     * identification, and notification settings.
     */
    @Getter
    @NoArgsConstructor
    public static class GrafanaManagedReceiverConfig {
        private String type;
        private String name;
        private String uid;
        private boolean disableResolveMessage;
        private Settings settings;
        private Map<String, Object> secureSettings;
    }

    /**
     * Settings nested class for MQTT broker configuration Contains MQTT connection parameters,
     * message formatting, and TLS settings.
     */
    @Getter
    @NoArgsConstructor
    public static class Settings {
        private String brokerUrl;
        private String message;
        private String messageFormat;
        private String qos;
        private boolean retain;
        private TlsConfig tlsConfig;
        private String topic;
        private String username;
    }

    /**
     * TLS configuration nested class for secure MQTT connections Contains TLS verification settings
     * for MQTT broker connections.
     */
    @Getter
    @NoArgsConstructor
    public static class TlsConfig {
        private boolean insecureSkipVerify;
    }
}
