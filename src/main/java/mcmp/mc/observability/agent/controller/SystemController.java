package mcmp.mc.observability.agent.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.dto.ResBody;
import mcmp.mc.observability.agent.loader.PluginLoader;
import mcmp.mc.observability.agent.model.PluginDefInfo;
import mcmp.mc.observability.agent.util.CollectorExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.PREFIX_V1)
@RequiredArgsConstructor
public class SystemController {
    private final PluginLoader pluginLoader;
    private final CollectorExecutor collectorExecutor;

    @GetMapping("/plugins")
    public ResBody getPlugins() {
        ResBody<List<PluginDefInfo>> resBody = new ResBody<>();
        resBody.setData(pluginLoader.getPluginDefList());
        return resBody;
    }

    @GetMapping("/state")
    public Boolean state() {
        return !collectorExecutor.isInactiveAgent();
    }
}
