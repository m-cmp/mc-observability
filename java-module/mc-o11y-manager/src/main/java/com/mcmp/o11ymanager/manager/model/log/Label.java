package com.mcmp.o11ymanager.manager.model.log;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 로그 레이블 도메인 모델 */
@Getter
@Builder
public class Label {
    private final String status;
    private final List<String> labels;
}
