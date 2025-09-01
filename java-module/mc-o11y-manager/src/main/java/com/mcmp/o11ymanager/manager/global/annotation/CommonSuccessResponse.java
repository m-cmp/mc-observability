package com.mcmp.o11ymanager.manager.global.annotation;

import com.mcmp.o11ymanager.manager.dto.common.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "공통 성공 응답",
                content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
})

public @interface CommonSuccessResponse {
}