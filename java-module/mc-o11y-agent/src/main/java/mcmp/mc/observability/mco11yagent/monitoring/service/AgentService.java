package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.TargetMapper;
import mcmp.mc.observability.mco11yagent.monitoring.util.CollectorExecutor;
import mcmp.mc.observability.mco11yagent.monitoring.util.Utils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@DependsOn("collectorExecutor")
@RequiredArgsConstructor
public class AgentService {
    @Value("${spring.datasource.url:0.0.0.0}")
    private String datasourceUrl;

    private final CollectorExecutor collectorExecutor;
    private final TargetMapper targetMapper;

    @PostConstruct
    public boolean init() {
        try {
            if( datasourceUrl.contains(Constants.EMPTY_HOST) ) return false;

            File confDirectory = new File(Constants.COLLECTOR_CONFIG_DIR_PATH);
            if (!confDirectory.exists() || !confDirectory.isDirectory()) {
                confDirectory.mkdir();
            }

            File confFile = new File(Constants.COLLECTOR_CONFIG_PATH);
            if (!confFile.exists()) {
                confFile.createNewFile();
            }

            Utils.writeFile(collectorExecutor.globalTelegrafConfig(), Constants.COLLECTOR_CONFIG_PATH);

            collectorExecutor.startCollector();

            String nsId = System.getProperty(Constants.PROPERTY_NS_ID);
            if( nsId == null ) nsId = "";
            String targetId = System.getProperty(Constants.PROPERTY_TARGET_ID);
            if( targetId == null ) targetId = "";
            targetMapper.updateState(nsId, targetId, "ACTIVE");
        }
        catch (IOException e) {
            log.error(ExceptionUtils.getMessage(e));
        }

        return true;
    }

    @PreDestroy
    public void destroy() {
        String nsId = System.getProperty(Constants.PROPERTY_NS_ID);
        if( nsId == null ) nsId = "";
        String targetId = System.getProperty(Constants.PROPERTY_TARGET_ID);
        if( targetId == null ) targetId = "";
        targetMapper.updateState(nsId, targetId, "INACTIVE");
    }
}
