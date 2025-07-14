package com.mcmp.o11ymanager.global.annotation;

import com.mcmp.o11ymanager.dto.common.ErrorResponse;
import com.mcmp.o11ymanager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.dto.history.HistoryResponseDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  // 적용 가능한 위치를 METHOD로 한정
@Retention(RetentionPolicy.RUNTIME)  // 런타임에도 유지됨
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "History 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(allOf = {SuccessResponse.class, HistoryResponseDTO.class}),
                        examples = {
                                @ExampleObject(
                                        name = "SuccessHistoryExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-03-10T10:35:56\",\n" +
                                                "  \"status\": \"SUCCESS\",\n" +
                                                "  \"code\": \"OK\",\n" +
                                                "  \"message\": \"정상적으로 처리되었습니다.\",\n" +
                                                "  \"requestId\": \"e9d1879a-c5c8-4684-b4b1-e459b8501f81\",\n" +
                                                "  \"data\": {\n" +
                                                "    \"result\": {\n" +
                                                "      \"id\": \"history-id-123\",\n" +
                                                "      \"timestamp\": \"2025-03-10T10:35:56\",\n" +
                                                "      \"action\": \"INSTALL\",\n" +
                                                "      \"hostResponseDTO\": {\n" +
                                                "          \"id\": \"8b3558e7-b4b8-460d-960a-10bc57b8ef6b\",\n" +
                                                "          \"hostname\": null,\n" +
                                                "          \"credentialId\": \"51cbd96c-797d-4ff1-a31b-1730ff5be0d2\",\n" +
                                                "          \"cloudService\": \"openstack\",\n" +
                                                "          \"vmId\": \"94c3e402-5042-4c8e-9248-cdfcb1462deb\",\n" +
                                                "          \"name\": \"ish-agent-test\",\n" +
                                                "          \"type\": \"vm\",\n" +
                                                "          \"ip\": \"192.168.110.28\",\n" +
                                                "          \"port\": 22,\n" +
                                                "          \"user\": \"root\",\n" +
                                                "          \"description\": \"Agent Test server\",\n" +
                                                "          \"hostStatus\": null,\n" +
                                                "          \"hostAgentTaskStatus\": null,\n" +
                                                "          \"monitoringServiceStatus\": null,\n" +
                                                "          \"logServiceStatus\": null,\n" +
                                                "          \"configGitHash\": null,\n" +
                                                "          \"agentVersion\": null,\n" +
                                                "          \"createdAt\": \"2025-03-10T10:35:56\",\n" +
                                                "          \"updatedAt\": \"2025-03-10T10:35:56\"\n" +
                                                "      }\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "ServerErrorExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-03-13T16:05:41\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"INTERNAL_SERVER_ERROR\",\n" +
                                                "  \"message\": \"서버 내부 오류가 발생했습니다.\",\n" +
                                                "  \"requestId\": \"523ba9da-d8d5-4f3b-b2ce-22306d7b9897\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        )
})


public @interface HistoryInfoResponse {
}