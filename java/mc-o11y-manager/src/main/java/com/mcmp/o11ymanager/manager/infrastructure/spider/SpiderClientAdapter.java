package com.mcmp.o11ymanager.manager.infrastructure.spider;

import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterList;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@RequiredArgsConstructor
public class SpiderClientAdapter implements SpiderClient {

    private final SpiderClient spiderClient;

    @Override
    public SpiderMonitoringInfo.Data getVMMonitoring(
            String vmName,
            String measurement,
            String connectionName,
            String timeBeforeHour,
            String intervalMinute) {
        return spiderClient.getVMMonitoring(
                vmName, measurement, connectionName, timeBeforeHour, intervalMinute);
    }

    @Override
    public SpiderMonitoringInfo.Data getClusterNodeMonitoring(
            String clusterName,
            String nodeGroupName,
            String nodeNumber,
            String measurement,
            String connectionName,
            String timeBeforeHour,
            String intervalMinute) {
        return spiderClient.getClusterNodeMonitoring(
                clusterName,
                nodeGroupName,
                nodeNumber,
                measurement,
                connectionName,
                timeBeforeHour,
                intervalMinute);
    }

    @Override
    public SpiderClusterList listClusters(String connectionName) {
        return spiderClient.listClusters(connectionName);
    }

    @Override
    public SpiderClusterInfo getCluster(String clusterName, String connectionName) {
        return spiderClient.getCluster(clusterName, connectionName);
    }
}
