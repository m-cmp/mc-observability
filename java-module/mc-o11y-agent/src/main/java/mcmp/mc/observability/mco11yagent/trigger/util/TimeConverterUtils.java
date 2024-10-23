package mcmp.mc.observability.mco11yagent.trigger.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeConverterUtils {

    public static final String toUTCFormat(String dateTimeStr){
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
        return dateTime.toInstant(ZoneOffset.UTC).toString();
    }
}
