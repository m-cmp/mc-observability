package com.mcmp.o11ymanager.infrastructure.spider;

import com.mcmp.o11ymanager.dto.SpiderMonitoringInfo;
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
      String intervalMinute
  ) {
    return spiderClient.getVMMonitoring(vmName, measurement, connectionName, timeBeforeHour, intervalMinute);
  }

}
