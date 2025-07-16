package com.mcmp.o11ymanager.port;

import com.mcmp.o11ymanager.dto.SpiderMonitoringInfo;

public interface SpiderPort {


  SpiderMonitoringInfo.Data getVMMonitoring(
      String vmName,
      String measurement,
      String connectionName,
      String timeBeforeHour,
      String intervalMinute
  );
}
