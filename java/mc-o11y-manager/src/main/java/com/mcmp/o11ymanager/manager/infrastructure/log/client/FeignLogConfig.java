package com.mcmp.o11ymanager.manager.infrastructure.log.client;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignLogConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    // Custom Logger
    @Bean
    Logger feignLogger() {
        return new DetailedFeignLogger(LokiFeignClient.class);
    }

    public static class DetailedFeignLogger extends Logger {

        private final org.slf4j.Logger logger;
        private final Class<?> loggerClass;

        public DetailedFeignLogger(Class<?> loggerClass) {
            this.loggerClass = loggerClass;
            this.logger = LoggerFactory.getLogger(loggerClass);
        }

        @Override
        protected void log(String configKey, String format, Object... args) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(methodTag(configKey) + format, args));
            }
        }

        @Override
        protected void logRequest(String configKey, Level logLevel, Request request) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        ">>> 요청 정보 [{}]: {} {}",
                        loggerClass.getSimpleName(),
                        request.httpMethod(),
                        request.url());

                if (request.headers() != null && !request.headers().isEmpty()) {
                    logger.debug(">>> 요청 헤더 [{}]:", loggerClass.getSimpleName());
                    request.headers()
                            .forEach(
                                    (name, values) ->
                                            values.forEach(
                                                    value ->
                                                            logger.debug(
                                                                    ">>>   {}: {}", name, value)));
                }
                if (request.body() != null) {
                    try {
                        String bodyText = new String(request.body(), StandardCharsets.UTF_8);
                        logger.debug(
                                ">>> Request Body [{}]: {}", loggerClass.getSimpleName(), bodyText);
                    } catch (Exception e) {
                        logger.debug(
                                ">>> Request Body [{}]: [Binary Data]",
                                loggerClass.getSimpleName());
                    }
                }
            }
        }

        @Override
        protected Response logAndRebufferResponse(
                String configKey, Level logLevel, Response response, long elapsedTime)
                throws IOException {
            int status = response.status();
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "<<< Response Status [{}]: {} ({}ms)",
                        loggerClass.getSimpleName(),
                        status,
                        elapsedTime);

                if (response.headers() != null && !response.headers().isEmpty()) {
                    logger.debug("<<< Response Header [{}]:", loggerClass.getSimpleName());
                    response.headers()
                            .forEach(
                                    (name, values) ->
                                            values.forEach(
                                                    value ->
                                                            logger.debug(
                                                                    "<<<   {}: {}", name, value)));
                }
            }

            if (response.body() != null) {
                byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                if (bodyData.length > 0 && logger.isDebugEnabled()) {
                    String bodyText = new String(bodyData, StandardCharsets.UTF_8);
                    if (bodyText.length() > 1000) {
                        logger.debug(
                                "<<< Response Body [{}] (partial): {}...",
                                loggerClass.getSimpleName(),
                                bodyText.substring(0, 1000));
                    } else {
                        logger.debug(
                                "<<< Response Body [{}]: {}",
                                loggerClass.getSimpleName(),
                                bodyText);
                    }
                }

                return response.toBuilder().body(bodyData).build();
            }

            return response;
        }
    }
}
