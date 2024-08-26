package mcmp.mc.observability.agent.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class JsonDateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Timestamp deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String date = jsonParser.getText();
        if (date == null || date.equals("")) return null;

        return Timestamp.valueOf(LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }


}
