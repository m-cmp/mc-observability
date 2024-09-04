package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.OS;
import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.PluginService;
import mcmp.mc.observability.mco11yagent.monitoring.util.Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static mcmp.mc.observability.mco11yagent.monitoring.common.Constants.COLLECTOR_CONFIG_DIR_PATH;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI)
public class SystemController {

    private final PluginService pluginService;

    @GetMapping("/plugins")
    public ResBody<List<PluginDefInfo>> list() {
        return pluginService.getList();
    }

    @GetMapping("/publicIp")
    public ResBody<Object> publicIp() throws IOException, InterruptedException {
        String data = "";

        switch (OS.parseProperty()) {
            case WINDOWS -> {
                data = Utils.runExec(new String[]{"powershell", "/c", "$(curl ifconfig.me).Content"});
            }
            case LINUX, UNIX -> {
                data = Utils.runExec(new String[]{"/bin/sh", "-c", "curl ifconfig.me"});
            }
        }
        return ResBody.builder().data(data).build();
    }
}
