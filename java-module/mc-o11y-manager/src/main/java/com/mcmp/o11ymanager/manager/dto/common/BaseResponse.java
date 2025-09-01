package com.mcmp.o11ymanager.manager.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.global.definition.TimestampDefinition;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

import java.util.Date;

@Getter
@Setter
@SuperBuilder
public abstract class BaseResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TimestampDefinition.TIMESTAMP_FORMAT)
    private Date timestamp;
    private ResponseStatus status;
    private ResponseCode code;
    private String message;
    private String requestId;

    protected <T extends BaseResponse> ResponseEntity<T> toResponseEntity() {
        return ResponseEntity
                .status(this.getCode().getHttpStatus())
                .body((T) this);
    }
} 