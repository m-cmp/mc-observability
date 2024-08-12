package mcmp.mc.observability.agent.monitoring.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.common.config.ApiProfileCondition;
import mcmp.mc.observability.agent.monitoring.enums.HostState;
import mcmp.mc.observability.agent.monitoring.enums.OS;
import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import mcmp.mc.observability.agent.common.util.CollectorExecutor;
import mcmp.mc.observability.agent.common.util.GlobalProperties;
import mcmp.mc.observability.agent.common.util.Utils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@DependsOn("globalProperties")
@Conditional(ApiProfileCondition.class)
@RequiredArgsConstructor
public class AgentInit implements InitializingBean, DisposableBean {
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    private final HostService hostService;
    private final CollectorExecutor collectorExecutor;
    private final GlobalProperties globalProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }
    private void init() throws IOException {
        try {

            if( datasourceUrl.contains(Constants.EMPTY_HOST) ) return;

            String ex = System.getProperty(Constants.EXTENSION_PROPERTY_KEY);
            log.info("{} : {}", Constants.EXTENSION_PROPERTY_KEY, ex);
            Gson gson = new Gson();
            JsonObject jsonEx = (ex == null? new JsonObject(): gson.fromJson(ex, JsonElement.class).getAsJsonObject());

            File confDirectory = new File(Constants.COLLECTOR_CONFIG_DIR_PATH);
            if( !confDirectory.exists() || !confDirectory.isDirectory() ) {
                confDirectory.mkdir();
            }

            File confFile = new File(Constants.COLLECTOR_CONFIG_PATH);
            if (!confFile.exists()){
                confFile.createNewFile();
            }

            String targetUuid = globalProperties.getUuid();

            String telegrafConfig = collectorExecutor.globalTelegrafConfig(targetUuid);
            Utils.writeFile(telegrafConfig, Constants.COLLECTOR_CONFIG_PATH);

            HostInfo hostInfo = HostInfo.builder()
                    .uuid(globalProperties.getUuid())
                    .os(OS.parseProperty())
                    .monitoringYn(StateYN.Y)
                    .state(HostState.ACTIVE)
                    .ex(jsonEx.toString())
                    .build();

            hostService.insertHost(hostInfo);

            hostInfo = hostService.getDetail(hostService.getHostSeq(hostInfo.getUuid()));

            if (hostInfo.getMonitoringYn() == StateYN.Y) {
                collectorExecutor.startAgent();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void shutdown() throws IOException {
        if( datasourceUrl.contains(Constants.EMPTY_HOST) ) return;

        HostInfo hostInfo = HostInfo.builder()
                .uuid(globalProperties.getUuid())
                .state(HostState.INACTIVE)
                .build();

        hostService.updateHost(hostInfo);
    }
}
