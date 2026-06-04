package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TumblebugInfra {
    private String id;

    private Node[] node;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        private String resourceType;

        private String id; // example: "aws-ap-southeast-1"

        private String uid; // example: "wef12awefadf1221edcf"

        private String cspResourceName; // example: "we12fawefadf1221edcf"

        private String cspResourceId; // example: "csp-06eb41e14121c550a"

        private String connectionName;

        private String name; // example: "aws-ap-southeast-1"

        private String nodeGroupId;

        private String description;

        private String nodeUserName;

        // Windows VM의 administrator password (평문 노출). cloud-init 완료 후 응답에서 마스킹/제거되는
        // 케이스가 있어 install API 시점엔 null인 경우도 있음 (별도 대응 필요).
        private String nodeUserPassword;

        private String publicIP;

        // OpenStack/사내망 VM은 floating IP가 자동 할당 안 되면 publicIP가 빈 문자열로 옴.
        // VmAccessInfoResolver가 publicIP 비면 privateIP로 fallback해 사용.
        private String privateIP;

        private String sshPort;

        private String sshKeyId;

        private String status;

        private String createdTime; // example: "2022-11-10 23:00:00"

        // cb-tumblebug NodeInfo.Image (ImageSummary). osPlatform이 비어있을 수 있어
        // osType/osDistribution 문자열에서 windows 키워드도 fallback으로 검사한다.
        private Image image;

        public boolean isWindows() {
            if (image == null) {
                return false;
            }
            if ("Windows".equalsIgnoreCase(image.getOsPlatform())) {
                return true;
            }
            return containsWindows(image.getOsType()) || containsWindows(image.getOsDistribution());
        }

        private static boolean containsWindows(String s) {
            return s != null && s.toLowerCase().contains("windows");
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String osType;
        private String osDistribution;
        private String osPlatform; // "Linux/UNIX", "Windows", "NA"
        private String osArchitecture;
    }
}
