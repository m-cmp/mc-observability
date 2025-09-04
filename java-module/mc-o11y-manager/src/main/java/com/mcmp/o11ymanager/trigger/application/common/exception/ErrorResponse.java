package com.mcmp.o11ymanager.trigger.application.common.exception;

import lombok.Builder;

@Builder
public record ErrorResponse(String errorCode, String message) {}
