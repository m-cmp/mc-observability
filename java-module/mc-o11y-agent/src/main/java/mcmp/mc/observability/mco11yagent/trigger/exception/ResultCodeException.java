package mcmp.mc.observability.mco11yagent.trigger.exception;

import lombok.Getter;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;

@Getter
public class ResultCodeException extends RuntimeException {
    private final ResultCode resultCode;
    private final Object[] objects;

    public ResultCodeException(ResultCode resultCode, String message, Object... objects) {
        super(message);
        this.resultCode = resultCode;
        this.objects = objects;
    }
}
