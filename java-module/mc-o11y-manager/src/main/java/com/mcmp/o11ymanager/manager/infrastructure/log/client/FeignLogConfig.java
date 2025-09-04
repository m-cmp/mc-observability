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

    /**
     * Feign 클라이언트의 로깅 레벨을 설정합니다. NONE: 로깅하지 않음 BASIC: 요청 메서드와 URL, 응답 상태 코드, 실행 시간만 로깅 HEADERS:
     * BASIC에 요청 및 응답 헤더를 추가하여 로깅 FULL: 요청 및 응답의 헤더, 본문, 메타데이터를 모두 로깅
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /** 상세한 로깅을 위한 커스텀 Feign 로거 */
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
                        logger.debug(">>> 요청 본문 [{}]: {}", loggerClass.getSimpleName(), bodyText);
                    } catch (Exception e) {
                        logger.debug(">>> 요청 본문 [{}]: [바이너리 데이터]", loggerClass.getSimpleName());
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
                        "<<< 응답 상태 [{}]: {} ({}ms)",
                        loggerClass.getSimpleName(),
                        status,
                        elapsedTime);

                if (response.headers() != null && !response.headers().isEmpty()) {
                    logger.debug("<<< 응답 헤더 [{}]:", loggerClass.getSimpleName());
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
                                "<<< 응답 본문 [{}] (일부): {}...",
                                loggerClass.getSimpleName(),
                                bodyText.substring(0, 1000));
                    } else {
                        logger.debug("<<< 응답 본문 [{}]: {}", loggerClass.getSimpleName(), bodyText);
                    }
                }
                return response.toBuilder().body(bodyData).build();
            }

            return response;
        }
    }
}
