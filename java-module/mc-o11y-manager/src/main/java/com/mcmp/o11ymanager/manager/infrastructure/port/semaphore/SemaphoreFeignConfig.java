package com.mcmp.o11ymanager.manager.infrastructure.port.semaphore;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.manager.global.util.CookieJar;
import feign.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collection;

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
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true);
        return mapper;
    }

    @Bean
    public Client feignClient(CookieJar cookieJar) {
        return new Client.Default(null, null) {
            @Override
            public Response execute(Request request, Request.Options options) throws IOException {
                Response response = super.execute(request, options);

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
            }
        };
    }

    @Bean
    Logger.Level semaphoreFeignLoggerLevel() {
        return Logger.Level.NONE;
    }
}
