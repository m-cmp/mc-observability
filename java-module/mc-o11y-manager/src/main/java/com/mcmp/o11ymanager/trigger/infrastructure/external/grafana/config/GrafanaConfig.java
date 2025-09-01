package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.config;


import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.GrafanaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration class for Grafana integration Sets up WebClient and GrafanaClient beans for
 * communicating with Grafana API.
 */
@Configuration
public class GrafanaConfig {

    @Value("${grafana.alert.protocol}")
    private String protocol;

    @Value("${grafana.alert.ip}")
    private String ip;

    @Value("${grafana.alert.port}")
    private String port;

    @Value("${grafana.alert.apiKey}")
    private String apiKey;

    /**
     * Creates a WebClient bean configured for Grafana API calls.
     *
     * @return WebClient configured with Grafana base URL and authorization header
     */
    @Bean
    public WebClient grafanaWebClient() {
        return WebClient.builder()
                .baseUrl(protocol + "://" + ip + ":" + port)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Creates a GrafanaClient bean using HTTP Service Proxy.
     *
     * @param grafanaWebClient the WebClient to use for HTTP requests
     * @return GrafanaClient proxy for making API calls
     */
    @Bean
    public GrafanaClient grafanaClient(WebClient grafanaWebClient) {
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(WebClientAdapter.create(grafanaWebClient))
                .build()
                .createClient(GrafanaClient.class);
    }
}
