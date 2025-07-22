package com.mcmp.o11ymanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcmp.o11ymanager.model.host.HostStatus;
import com.mcmp.o11ymanager.oldService.domain.TcpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.test.util.ReflectionTestUtils;

public class TcpServiceTest {

  TcpServiceImpl tcpService = new TcpServiceImpl();

  private final String UNREACHABLE_IP = "192.0.2.0";
  private final int CLOSED_PORT = 22;

  private final String LOCAL_IP = "192.168.110.28";
  private final int OPEN_PORT = 22;

  private final int TEST_TIMEOUT = 10000; // 테스트용 짧은 타임아웃

  @BeforeEach
  void setUp() {
    tcpService = new TcpServiceImpl();
    ReflectionTestUtils.setField(tcpService, "hostConnectionCheckTimeout", TEST_TIMEOUT);
  }


  @Test
  @Tag("success-case")
  @DisplayName("isConnect: 실제 연결이 가능한 서버에 연결 시 true를 반환해야 한다")
  void isConnect_shouldReturnTrue_whenActualConnectionSuccessful() {
    boolean result = tcpService.isConnect(LOCAL_IP, OPEN_PORT);
    assertTrue(result, "실제 연결이 가능할 때 true를 반환해야 합니다. (로컬 서버 열림 확인)");
  }

  @Test
  @Tag("failure-case")
  @DisplayName("isConnect: 연결할 수 없는 IP/Port로 연결 시 false를 반환해야 한다")
  void isConnect_shouldReturnFalse_whenConnectionFails() {
    boolean result = tcpService.isConnect(UNREACHABLE_IP, CLOSED_PORT);
    assertFalse(result, "연결 실패 시 false를 반환해야 합니다.");
  }

  @Test
  @Tag("failure-case")
  @DisplayName("isConnect: 존재하지 않는 IP로 연결 시 타임아웃 후 false를 반환해야 한다")
  void isConnect_shouldReturnFalse_whenNonExistentIp() {
    long startTime = System.currentTimeMillis();
    boolean result = tcpService.isConnect("10.255.255.1", 12345); // 일반적으로 존재하지 않는 사설 IP
    long endTime = System.currentTimeMillis();

    assertFalse(result, "존재하지 않는 IP는 연결 실패로 false를 반환해야 합니다.");
    assertTrue(endTime - startTime >= TEST_TIMEOUT - 100 && endTime - startTime <= TEST_TIMEOUT + 1000,
        "연결 시도가 대략 타임아웃 시간만큼 걸렸는지 확인");
  }



  @Test
  @Tag("success-case")
  @DisplayName("checkServerStatus: 실제 연결이 가능한 서버에 연결 시 RUNNING 상태를 반환해야 한다")
  void checkServerStatus_shouldReturnRunning_whenActualConnectionSuccessful() {
    HostStatus status = tcpService.checkServerStatus(LOCAL_IP, OPEN_PORT);
    assertEquals(HostStatus.RUNNING, status, "실제 연결이 가능할 때 RUNNING 상태를 반환해야 합니다.");
  }


  @Test
  @Tag("failure-case")
  @DisplayName("checkServerStatus: 연결할 수 없는 IP/Port로 연결 시 FAILED 상태를 반환해야 한다")
  void checkServerStatus_shouldReturnFailed_whenConnectionFails() {
    HostStatus status = tcpService.checkServerStatus(UNREACHABLE_IP, CLOSED_PORT);
    assertEquals(HostStatus.FAILED, status, "연결 실패 시 FAILED 상태를 반환해야 합니다.");
  }



}
