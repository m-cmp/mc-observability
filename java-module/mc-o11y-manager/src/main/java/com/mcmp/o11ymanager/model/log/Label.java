package com.mcmp.o11ymanager.model.log;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 로그 레이블 도메인 모델
 */
@Getter
@Builder
public class Label {
    private final String status;
    private final List<String> labels;
} 