package com.innogrid.tabcloudit.o11ymanager.global.annotation;

import com.innogrid.tabcloudit.o11ymanager.dto.common.ErrorResponse;
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
        @ApiResponse(responseCode = "500", description = "실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})

public @interface CommonErrorResponse {
}


