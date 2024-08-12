package mcmp.mc.observability.agent.monitoring.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS("0000", "완료되었습니다."),
    SUCCESS_NO_DATA("0000", "데이터가 없습니다."),


    DATABASE_ERROR("9993", "데이터베이스 오류가 발생했습니다."),
    FAILED("9994", "실패하였습니다."),
    NOT_FOUND_DATA( "9995", "데이터가 존재하지 않습니다."),
    INVAILD_PARAMETER("9996", "파라미터를 확인해주세요."),
    NOT_FOUND_REQUIRED("9997", "필수 항목을 확인해주세요."),
    INVALID_REQUEST("9998", "잘못된 요청입니다."),
    INVALID_ERROR("9999", "알 수 없는 오류가 발생했습니다."),
    ;

    private final String code;
    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}