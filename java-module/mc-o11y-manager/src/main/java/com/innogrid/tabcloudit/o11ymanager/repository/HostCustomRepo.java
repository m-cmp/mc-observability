package com.innogrid.tabcloudit.o11ymanager.repository;

import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.exception.host.HostNotExistException;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Pattern;


@Repository
@RequiredArgsConstructor
@Slf4j
public class HostCustomRepo {

  private final HostJpaRepository hostJpaRepository;


  public HostEntity save(String requestId, HostEntity input, boolean isUpdate) {
    LocalDateTime now = LocalDateTime.now();
    HostEntity entity;
//
//    if (isUpdate) {
//      entity = hostJpaRepository.findById(input.getId())
//          .orElseThrow(() -> new HostNotExistException(requestId, input.getId()));
//
//      // 2) 수정 가능한 필드 덮어쓰기
//      if (input.getPort() != 0 && input.getPort() > 0) {
//        entity.setPort(input.getPort());
//      }
//      if (input.getUser() != null && !input.getUser().isBlank()) {
//        entity.setUser(input.getUser());
//      }
//
//      // 3) 비밀번호 변경 로직
//      String rawPwd = input.getPassword();
//      if (rawPwd != null && !rawPwd.isBlank()) {
//        try {
//          // 기존 저장된 값 복호화(또는 그대로)
//          String oldPlain = isValidBase64(entity.getPassword())
//              ? ChaCha20Poly3105Util.decryptString(entity.getPassword())
//              : entity.getPassword();
//          // 입력된 rawPwd 복호화(또는 그대로)
//          String newPlain = isValidBase64(rawPwd)
//              ? ChaCha20Poly3105Util.decryptString(rawPwd)
//              : rawPwd;
//          // 달라졌을 때만 암호화
//          if (!newPlain.equals(oldPlain)) {
//            entity.setPassword(ChaCha20Poly3105Util.encryptString(newPlain));
//          }
//        } catch (Exception e) {
//          throw new RuntimeException("Password encryption/decryption error", e);
//        }
//      }
//
//    } else {
//      // 신규 생성
//      entity = input;
//      entity.setCreatedAt(now);
//
//      // 암호화
//      if (entity.getPassword() != null && !entity.getPassword().isBlank()) {
//        try {
//          entity.setPassword(
//              ChaCha20Poly3105Util.encryptString(entity.getPassword())
//          );
//        } catch (Exception e) {
//          throw new RuntimeException("Password encryption failed", e);
//        }
//      }
//    }
//
//    // 4) 항상 updatedAt 갱신
//    entity.setUpdatedAt(now);

    // 5) 저장
    return hostJpaRepository.save(input);
  }


  public boolean isValidBase64(String input) {
    if (input == null || input.isEmpty()) {
      return false;
    }

    if (!isBase64(input)) {
      return false;
    }

    try {
      Base64.getDecoder().decode(input);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean isBase64(String input) {
    if (input == null || input.isEmpty()) {
      return false;
    }

    String regex = "^[A-Za-z0-9+/]*={0,2}$";

    if (!Pattern.matches(regex, input)) {
      return false;
    }

    return input.length() % 4 == 0;
  }


}


