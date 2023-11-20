package mcmp.mc.observability.agent.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.enums.TelegrafState;
import mcmp.mc.observability.agent.service.HostService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorExecutor {

    private final HostService hostService;
    private final GlobalProperties globalProperties;
    private Process AGENT_PROCESS = null;
    private final ClassPathResource globalConfigResource = new ClassPathResource("defaultGlobalConfig");

    @PreDestroy
    public void stopAgent() {
        if( AGENT_PROCESS == null ) return;
        AGENT_PROCESS.destroy();

        String uuid = globalProperties.getUuid();
        Long hostSeq = hostService.getHostSeq(uuid);
        hostService.updateTelegrafState(hostSeq, TelegrafState.STOPPED);
    }

    public void startAgent() {
        try {
            TelegrafState state = TelegrafState.RUNNING;
            if( AGENT_PROCESS == null || !AGENT_PROCESS.isAlive() ) {
                Process once = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll", "--once").start();
                once.waitFor();

                if( once.exitValue() != 0) {
                    state = TelegrafState.FAILED;
                }else {
                    AGENT_PROCESS = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll").start();
                }
            }

            String uuid = globalProperties.getUuid();
            Long hostSeq = hostService.getHostSeq(uuid);

            hostService.updateTelegrafState(hostSeq, state);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public TelegrafState getTelegrafState() {
        if(AGENT_PROCESS != null) {
            if(!AGENT_PROCESS.isAlive()) {
                if (AGENT_PROCESS.exitValue() != 0) {
                    return TelegrafState.FAILED;
                } else {
                    return TelegrafState.STOPPED;
                }
            }
            return TelegrafState.RUNNING;
        }
        return TelegrafState.STOPPED;
    }

    public String globalTelegrafConfig(String receiverId) {
        StringBuilder sb = new StringBuilder();

        if(!globalConfigResource.exists()){
            log.error("Invalid filePath : defaultGlobalConfig");
            throw new IllegalArgumentException();
        }

        log.info("file path exists = {}", globalConfigResource.exists());

        try (InputStream is = new BufferedInputStream(globalConfigResource.getInputStream())) {
            String text = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            sb.append(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sb.toString().replaceAll("@UUID", receiverId);
    }
}
