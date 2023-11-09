package mcmp.mc.observability.agent.enums.converter;

import mcmp.mc.observability.agent.enums.HostState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HostStateConverter implements Converter<String, HostState> {
    @Override
    public HostState convert(String source) {
        return HostState.parse(source);
    }
}