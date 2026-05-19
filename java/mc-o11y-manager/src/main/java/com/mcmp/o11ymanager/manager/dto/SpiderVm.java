package com.mcmp.o11ymanager.manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Spider {@code GET /spider/vm/{vmName}?ConnectionName=...} 응답에서 필요한 IP 정보만 매핑.
 *
 * <p><b>왜 필요한가:</b> CB-Tumblebug v0.12.9가 OpenStack VM의 multi-IP를 자기 응답에 정확히 노출하지
 * 않고 {@code publicIP}를 빈 문자열로 채우는 케이스가 있다 (privateIP는 internal tenant network IP만
 * 들어옴). Spider raw 응답에는 OpenStack이 알려준 public/private IP가 모두 들어있어 fallback 소스로
 * 사용 가능.
 *
 * <p>Tumblebug VM 이름 ↔ Spider VM 이름 매핑: Tumblebug 응답의 {@code .node[0].cspResourceName}
 * 또는 {@code .node[0].uid}가 Spider 이름과 일치 (구체 필드명은 환경 확인 필요).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiderVm {
    // Spider 응답의 PascalCase 키 그대로. Tumblebug는 camelCase이므로 헷갈리지 말 것.
    private String PublicIP;
    private String PrivateIP;
    private String AccessPoint;
}
