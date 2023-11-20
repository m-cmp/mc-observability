package mcmp.mc.observability.agent.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.config.ApiProfileCondition;
import mcmp.mc.observability.agent.enums.HostState;
import mcmp.mc.observability.agent.enums.OS;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.model.HostInfo;
import mcmp.mc.observability.agent.util.CollectorExecutor;
import mcmp.mc.observability.agent.util.GlobalProperties;
import mcmp.mc.observability.agent.util.Utils;
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
            // CMP Agent가 설치 된 호스트 타입 (cmp/virtual-machine/node)

            String ex = System.getProperty("m-cmp-agent.ex");
            log.info("m-cmp-agent.ex : {}", ex);
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
