package com.mcmp.o11ymanager.manager.infrastructure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

public class JwtUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getRequestUserId(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("토큰이 필요합니다.");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("유효하지 않은 토큰 형식입니다.");
        }

        String payload = parts[1];
        byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
        try {
            Map<String, Object> claims = objectMapper.readValue(decodedBytes, Map.class);
            // "infos" 객체를 추출
            Map<String, Object> infos = (Map<String, Object>) claims.get("infos");
            // infos 객체에서 "username" 값을 추출하여 반환
            return infos != null ? (String) infos.get("username") : null;
        } catch (Exception e) {
            throw new RuntimeException("토큰 페이로드 디코딩에 실패했습니다.", e);
        }
    }
}
