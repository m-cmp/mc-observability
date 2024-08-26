package mcmp.mc.observability.agent.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonDateSerializer extends JsonSerializer<Date> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        jgen.writeString(DATE_FORMAT.format(value));
    }

}
