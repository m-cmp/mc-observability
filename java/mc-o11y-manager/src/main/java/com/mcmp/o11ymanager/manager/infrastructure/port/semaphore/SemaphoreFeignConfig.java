package com.mcmp.o11ymanager.manager.infrastructure.port.semaphore;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.manager.global.util.CookieJar;
import feign.*;
import feign.hc5.ApacheHttp5Client;
import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemaphoreFeignConfig {

    @Bean
    public CookieJar cookieJar() {
        return new CookieJar();
    }

    @Bean
    public RequestInterceptor cookieInterceptor(CookieJar cookieJar) {
        return template -> {
            if (!cookieJar.getCookies().isEmpty()) {
                template.header("Cookie", cookieJar.getCookiesAsString());
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register classpath modules (incl. ParameterNamesModule) so Jackson can bind JSON to
        // multi-arg constructors using the -parameters constructor names. Without this, decoding
        // Semaphore responses (e.g. Project) fails with "constructor ... has no property name",
        // which silently broke Semaphore initialization on startup.
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true);
        return mapper;
    }

    /**
     * Cookie-capturing client shared by every Feign client in the app.
     *
     * <p>This bean is picked up by component scan, so it wins over Spring Cloud's
     * {@code @ConditionalOnMissingBean} client auto-configuration for all Feign clients, not just
     * Semaphore. It therefore must support every method the other clients use: delegating to {@link
     * ApacheHttp5Client} instead of extending {@code Client.Default} is what makes PATCH work —
     * {@code Client.Default} runs on {@code HttpURLConnection}, which rejects PATCH with "Invalid
     * HTTP method" and broke the Insight proxy's PATCH endpoints.
     */
    @Bean
    public Client feignClient(CookieJar cookieJar) {
        Client delegate = new ApacheHttp5Client();
        return (request, options) -> {
            Response response = delegate.execute(request, options);

            Collection<String> setCookieHeaders = response.headers().get("Set-Cookie");
            if (setCookieHeaders != null) {
                for (String header : setCookieHeaders) {
                    String[] cookieParts = header.split(";")[0].split("=");
                    if (cookieParts.length == 2) {
                        cookieJar.addCookie(cookieParts[0], cookieParts[1]);
                    }
                }
            }

            return response;
        };
    }

    @Bean
    Logger.Level semaphoreFeignLoggerLevel() {
        return Logger.Level.NONE;
    }
}
