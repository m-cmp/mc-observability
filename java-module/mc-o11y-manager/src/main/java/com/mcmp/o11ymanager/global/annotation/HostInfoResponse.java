package com.mcmp.o11ymanager.global.annotation;

import com.mcmp.o11ymanager.dto.common.ErrorResponse;
import com.mcmp.o11ymanager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.dto.host.HostResponseDTO;
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
                description = "HOST 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(allOf = {SuccessResponse.class, HostResponseDTO.class}),
                        examples = {
                                @ExampleObject(
                                        name = "SuccessExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-03-10T10:35:56\",\n" +
                                                "  \"status\": \"SUCCESS\",\n" +
                                                "  \"code\": \"OK\",\n" +
                                                "  \"message\": \"정상적으로 처리되었습니다.\",\n" +
                                                "  \"requestId\": \"e9d1879a-c5c8-4684-b4b1-e459b8501f81\",\n" +
                                                "  \"data\": {\n" +
                                                "    \"result\": {\n" +
                                                "      \"id\": \"51cbd96c-797d-4ff1-a31b-1730ff5be0d2:399a40b1-e046-46cb-b0a8-77989adb1470\",\n" +
                                                "      \"hostname\": null,\n" +
                                                "      \"credentialId\": \"51cbd96c-797d-4ff1-a31b-1730ff5be0d2\",\n" +
                                                "      \"cloudService\": \"openstack\",\n" +
                                                "      \"vmId\": \"94c3e402-5042-4c8e-9248-cdfcb1462deb\",\n" +
                                                "      \"name\": \"ish-agent-test\",\n" +
                                                "      \"type\": \"vm\",\n" +
                                                "      \"ip\": \"192.168.110.28\",\n" +
                                                "      \"port\": 22,\n" +
                                                "      \"user\": \"root\",\n" +
                                                "      \"description\": \"Agent Test server\",\n" +
                                                "      \"hostStatus\": null,\n" +
                                                "      \"hostAgentTaskStatus\": null,\n" +
                                                "      \"monitoringServiceStatus\": null,\n" +
                                                "      \"logServiceStatus\": null,\n" +
                                                "      \"configGitHash\": null,\n" +
                                                "      \"agentVersion\": null,\n" +
                                                "      \"createdAt\": \"2025-03-10T10:35:56\",\n" +
                                                "      \"updatedAt\": \"2025-03-10T10:35:56\"\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "404",
                description = "호스트를 찾을 수 없습니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "HostNotFoundExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:00:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"1001\",\n" +
                                                "  \"message\": \"호스트를 찾을 수 없습니다.\",\n" +
                                                "  \"requestId\": \"uuid-404-host\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = "이미 존재하는 IP입니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "DuplicateHostExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:01:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"1002\",\n" +
                                                "  \"message\": \"이미 존재하는 IP입니다.\",\n" +
                                                "  \"requestId\": \"uuid-409-ip\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = "호스트에서 에이전트 관련 작업이 진행 중입니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "AgentTaskInProgressExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:03:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"1004\",\n" +
                                                "  \"message\": \"호스트에서 에이전트 관련 작업이 진행 중입니다.\",\n" +
                                                "  \"requestId\": \"uuid-409-agent\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "에이전트 작업 처리 중 실패했습니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "AgentFailureExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:10:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"2001\",\n" +
                                                "  \"message\": \"에이전트 작업 처리 중 실패했습니다.\",\n" +
                                                "  \"requestId\": \"uuid-500-agent\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "올바르지 않은 에이전트 타입입니다!",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "AgentTypeErrorExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:11:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"2002\",\n" +
                                                "  \"message\": \"올바르지 않은 에이전트 타입입니다!\",\n" +
                                                "  \"requestId\": \"uuid-400-type\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Config 베이스 디렉토리 생성에 실패했습니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "ConfigInitFailureExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:12:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"3001\",\n" +
                                                "  \"message\": \"Config 베이스 디렉토리 생성에 실패했습니다.\",\n" +
                                                "  \"requestId\": \"uuid-500-config\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "404",
                description = "해당 호스트에 에이전트 설정이 존재하지 않습니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "AgentConfigNotFoundExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:13:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"3002\",\n" +
                                                "  \"message\": \"해당 호스트에 에이전트 설정이 존재하지 않습니다.\",\n" +
                                                "  \"requestId\": \"uuid-404-config\",\n" +
                                                "  \"errors\": []\n" +
                                                "}"
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "404",
                description = "유효하지 않은 경로입니다.",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "ConfigNotFoundExample",
                                        value = "{\n" +
                                                "  \"timestamp\": \"2025-04-14T10:14:00\",\n" +
                                                "  \"status\": \"ERROR\",\n" +
                                                "  \"code\": \"3003\",\n" +
                                                "  \"message\": \"유효하지 않은 경로입니다.\",\n" +
                                                "  \"requestId\": \"uuid-404-path\",\n" +
                                                "  \"errors\": []\n" +
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


public @interface HostInfoResponse {
}