package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;

public interface SpiderPort {


  SpiderMonitoringInfo.Data getVMMonitoring(
      String vmName,
      String measurement,
      String connectionName,
      String timeBeforeHour,
      String intervalMinute
  );
}
