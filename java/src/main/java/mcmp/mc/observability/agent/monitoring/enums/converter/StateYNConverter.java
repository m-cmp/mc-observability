package mcmp.mc.observability.agent.monitoring.enums.converter;

import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class StateYNConverter implements Converter<String, StateYN> {
    @Override
    public StateYN convert(String source) {
        return StateYN.parse(source);
    }
}
