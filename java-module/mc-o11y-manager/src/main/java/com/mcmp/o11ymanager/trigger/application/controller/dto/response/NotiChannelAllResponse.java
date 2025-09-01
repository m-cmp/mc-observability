package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import java.util.List;

public record NotiChannelAllResponse(List<NotiChannelDetailDto> notiChannels) {
    public static NotiChannelAllResponse from(List<NotiChannelDetailDto> dtos) {
        return new NotiChannelAllResponse(dtos);
    }
}
