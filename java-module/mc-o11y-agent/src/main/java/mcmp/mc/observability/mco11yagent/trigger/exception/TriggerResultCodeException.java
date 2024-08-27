package mcmp.mc.observability.mco11yagent.trigger.exception;

import lombok.Getter;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;

@Getter
public class TriggerResultCodeException extends RuntimeException {
    private final ResultCode resultCode;
    private final Object[] objects;

    public TriggerResultCodeException(ResultCode resultCode, String message, Object... objects) {
        super(message);
        this.resultCode = resultCode;
        this.objects = objects;
    }
}
