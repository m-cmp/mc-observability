package com.mcmp.o11ymanager.manager.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Host Error
    HOST_NOT_EXISTS("1001", "호스트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_HOST("1002", "이미 존재하는 IP입니다.", HttpStatus.CONFLICT),
    RESOURCE_NOT_EXISTS("1003", "ID를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AGENT_TASK_IN_PROGRESS("1004", "호스트에서 에이전트 관련 작업이 진행 중입니다.", HttpStatus.CONFLICT),
    HOST_CONNECTION_FAILED("1005", "호스트에 연결할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // Agent Error
    AGENT_FAILURE("2001", " 에이전트 작업 처리 중 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AGENT_TYPE_ERROR("2002", "올바르지 않은 에이전트 타입입니다!", HttpStatus.BAD_REQUEST),
    MONITORING_AGENT_NOT_EXIST("2003", "모니터링 에이전트가 존재하지 않습니다", HttpStatus.BAD_REQUEST),
    LOG_AGENT_NOT_EXIST("2004", "로그 에이전트가 존재하지 않습니다", HttpStatus.BAD_REQUEST),
    AGENT_STATUS_ERROR("2005", "에이전트 상태 변경 명령에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Config, File Error
    CONFIG_INIT_FAILURE("3001", "Config 베이스 디렉토리 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AGENT_CONFIG_NOT_FOUND("3002", "해당 호스트에 에이전트 설정이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    CONFIG_NOT_FOUND("3003", "유효하지 않은 경로입니다.", HttpStatus.NOT_FOUND),
    FILE_READING("3004", "파일 로딩에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FAILED_DELETE_FILE("3005", "파일 삭제를 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DIRECTORY_NOT_FOUND("3005", "파일 삭제를 실패했습니다.", HttpStatus.NOT_FOUND),

    // state Error
    SSH_CONNECTION_FAILED("5001", "접속 정보 오류", HttpStatus.INTERNAL_SERVER_ERROR),

    // Git Error
    GIT_FILE_NOT_FOUND("6000", "Git 파일을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_INIT_FAILURE("6001", "Git 초기화에 실패했습니다.", HttpStatus.BAD_REQUEST),
    GIT_HASH_NOT_FOUND("6002", "Git Hash를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_TREE_WALK_FAILURE("6003", "Git Tree Walk에 실패했습니다.", HttpStatus.BAD_REQUEST),
    GIT_HEAD_REF_NOT_FOUND("6004", "Git HEAD Ref를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_REVERT_FAILURE("6005", "Git Revert에 실패했습니다.", HttpStatus.BAD_REQUEST),
    GIT_OBJECT_ID_NOT_FOUND("6006", "Git Object ID를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_REV_TREE_NOT_FOUND("6007", "Git Rev Tree를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_COMMIT_CONTENT_NOT_FOUND("6008", "Git Commit Content를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_HISTORY_NOT_FOUND("6009", "Git History를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    GIT_COMMIT_FAILURE("6010", "Git Commit에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // Loki Error
    LOKI_TIME_RANGE_EXCEEDED("7001", "조회 시간 범위가 Loki 서버의 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
