package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.PluginMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper pluginMapper;

    public ResBody<List<PluginDefInfo>> getList() {
        ResBody<List<PluginDefInfo>> resBody = new ResBody<>();
        resBody.setData(pluginMapper.getList());
        return resBody;
    }
}
