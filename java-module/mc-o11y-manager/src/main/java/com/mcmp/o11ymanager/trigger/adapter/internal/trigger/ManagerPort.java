package com.mcmp.o11ymanager.trigger.adapter.internal.trigger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


public interface ManagerPort {

  String getInfluxUid(String nsId, String targetScope, String targetId);

}
