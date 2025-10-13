package com.mcmp.o11ymanager.manager.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Host Error
    HOST_NOT_EXISTS("1001", "Host not found.", HttpStatus.NOT_FOUND),
    DUPLICATE_HOST("1002", "IP address already exists.", HttpStatus.CONFLICT),
    RESOURCE_NOT_EXISTS("1003", "ID not found.", HttpStatus.NOT_FOUND),
    AGENT_TASK_IN_PROGRESS(
            "1004", "Agent task is currently in progress on the host.", HttpStatus.CONFLICT),
    HOST_CONNECTION_FAILED("1005", "Unable to connect to host.", HttpStatus.BAD_REQUEST),

    // Agent Error
    AGENT_FAILURE("2001", "Failed to process agent task.", HttpStatus.INTERNAL_SERVER_ERROR),
    AGENT_TYPE_ERROR("2002", "Invalid agent type.", HttpStatus.BAD_REQUEST),
    MONITORING_AGENT_NOT_EXIST("2003", "Monitoring agent does not exist.", HttpStatus.BAD_REQUEST),
    LOG_AGENT_NOT_EXIST("2004", "Log agent does not exist.", HttpStatus.BAD_REQUEST),
    AGENT_STATUS_ERROR("2005", "Failed to change agent status.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Config, File Error
    CONFIG_INIT_FAILURE(
            "3001", "Failed to create config base directory.", HttpStatus.INTERNAL_SERVER_ERROR),
    AGENT_CONFIG_NOT_FOUND(
            "3002", "Agent configuration does not exist on the host.", HttpStatus.NOT_FOUND),
    CONFIG_NOT_FOUND("3003", "Invalid path.", HttpStatus.NOT_FOUND),
    FILE_READING("3004", "Failed to load file.", HttpStatus.INTERNAL_SERVER_ERROR),
    FAILED_DELETE_FILE("3005", "Failed to delete file.", HttpStatus.INTERNAL_SERVER_ERROR),
    DIRECTORY_NOT_FOUND("3006", "Directory not found.", HttpStatus.NOT_FOUND),

    // State Error
    SSH_CONNECTION_FAILED(
            "5001", "Connection information error.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Git Error
    GIT_FILE_NOT_FOUND("6000", "Git file not found.", HttpStatus.BAD_REQUEST),
    GIT_INIT_FAILURE("6001", "Failed to initialize Git.", HttpStatus.BAD_REQUEST),
    GIT_HASH_NOT_FOUND("6002", "Git hash not found.", HttpStatus.BAD_REQUEST),
    GIT_TREE_WALK_FAILURE("6003", "Failed to perform Git tree walk.", HttpStatus.BAD_REQUEST),
    GIT_HEAD_REF_NOT_FOUND("6004", "Git HEAD ref not found.", HttpStatus.BAD_REQUEST),
    GIT_REVERT_FAILURE("6005", "Failed to revert Git changes.", HttpStatus.BAD_REQUEST),
    GIT_OBJECT_ID_NOT_FOUND("6006", "Git object ID not found.", HttpStatus.BAD_REQUEST),
    GIT_REV_TREE_NOT_FOUND("6007", "Git revision tree not found.", HttpStatus.BAD_REQUEST),
    GIT_COMMIT_CONTENT_NOT_FOUND("6008", "Git commit content not found.", HttpStatus.BAD_REQUEST),
    GIT_HISTORY_NOT_FOUND("6009", "Git history not found.", HttpStatus.BAD_REQUEST),
    GIT_COMMIT_FAILURE("6010", "Failed to commit in Git.", HttpStatus.BAD_REQUEST),

    // Loki Error
    LOKI_TIME_RANGE_EXCEEDED(
            "7001",
            "The query time range exceeds the limit of the Loki server.",
            HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
