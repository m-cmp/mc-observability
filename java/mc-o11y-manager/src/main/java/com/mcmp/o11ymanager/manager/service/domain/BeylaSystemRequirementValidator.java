package com.mcmp.o11ymanager.manager.service.domain;

import com.mcmp.o11ymanager.manager.exception.agent.BeylaSystemRequirementException;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeylaSystemRequirementValidator {

    private static final String MINIMUM_KERNEL_VERSION = "5.8.0";     // 사실 eBPF

    private final TumblebugService tumblebugService;

    public BeylaSystemCheckResult validate(String nsId, String mciId, String vmId) {
        log.info("Validating Beyla system requirements for VM: {}/{}/{}", nsId, mciId, vmId);

        String kernelVersion = getKernelVersion(nsId, mciId, vmId);
        boolean btfSupported = checkBtfSupport(nsId, mciId, vmId);
        boolean kernelVersionValid = isKernelVersionValid(kernelVersion);

        BeylaSystemCheckResult result = BeylaSystemCheckResult.builder()
                .kernelVersion(kernelVersion)
                .btfSupported(btfSupported)
                .kernelVersionValid(kernelVersionValid)
                .minimumKernelVersion(MINIMUM_KERNEL_VERSION)
                .build();

        log.info("Beyla system check result: {}", result);

        return result;
    }

    public void validateAndThrow(String nsId, String mciId, String vmId) {
        BeylaSystemCheckResult result = validate(nsId, mciId, vmId);

        if (!result.isKernelVersionValid()) {
            throw new BeylaSystemRequirementException(
                    String.format(
                            "Kernel version %s is not supported. Minimum required: %s",
                            result.getKernelVersion(),
                            MINIMUM_KERNEL_VERSION),
                    result.getKernelVersion(),
                    result.isBtfSupported(),
                    MINIMUM_KERNEL_VERSION);
        }

        if (!result.isBtfSupported()) {
            throw new BeylaSystemRequirementException(
                    String.format(
                            "BTF (BPF Type Format) is not supported on this system. "
                                    + "Kernel version: %s. BTF requires kernel 5.2+ with CONFIG_DEBUG_INFO_BTF=y",
                            result.getKernelVersion()),
                    result.getKernelVersion(),
                    false,
                    MINIMUM_KERNEL_VERSION);
        }

        log.info(
                "Beyla system requirements validated successfully. Kernel: {}, BTF: {}",
                result.getKernelVersion(),
                result.isBtfSupported());
    }

    private String getKernelVersion(String nsId, String mciId, String vmId) {
        try {
            String result = tumblebugService.executeCommand(nsId, mciId, vmId, "uname -r");     // 원격 VM에 'uname -r'을 실행 => '5.15.0-91-generic' 같은 문자열이 들어옴
            return result != null ? result.trim() : "unknown";
        } catch (Exception e) {
            log.error("Failed to get kernel version: {}", e.getMessage());
            return "unknown";
        }
    }

    private boolean checkBtfSupport(String nsId, String mciId, String vmId) {
        try {
            String result =
                    tumblebugService.executeCommand(
                            nsId, mciId, vmId, "test -f /sys/kernel/btf/vmlinux && echo 'true' || echo 'false'");   // 원격 vm에 해당 문자열 입력, BTF 팡리이 있으면 true 없으면 false
            return result != null && result.trim().equals("true");  // trim()으로 앞뒤 공백 개행 문자 제거 후 비교 " 5.1" => "5.1" (주의 : trim은 중간의 공백은 제거하지 않음)
        } catch (Exception e) {
            log.error("Failed to check BTF support: {}", e.getMessage());
            return false;
        }
    }

    private boolean isKernelVersionValid(String kernelVersion) {
        if (kernelVersion == null || kernelVersion.equals("unknown")) {
            return false;
        }

        try {
            int[] current = parseKernelVersion(kernelVersion);
            int[] minimum = parseKernelVersion(MINIMUM_KERNEL_VERSION);

            for (int i = 0; i < Math.min(current.length, minimum.length); i++) {
                if (current[i] > minimum[i]) {
                    return true;
                } else if (current[i] < minimum[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse kernel version: {}", kernelVersion);
            return false;
        }
    }

    private int[] parseKernelVersion(String version) {                                     // "5.15.0-91-generic" 이란 문자열을 숫자로 파싱
        String[] parts = version.split("[-+]")[0].split("\\.");
        // split("[-+]")[0] : 하이픈 이나 + 뒤의 suffix 제거 => "5.15.0"
        // split("\\.") : 각 문자를 배열의 원소로 넣음 ["5", "15", "0"]
        int[] result = new int[3];

        for (int i = 0; i < Math.min(parts.length, 3); i++) {  // 커널 버전 문자열이 늘 3이라는 보장이 없어서 (5.2 같이) 3과 커널 버전 문자열 중 작은 버전으로 돌린다
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
                // 각 문자를 String -> int로 파싱 ["5", "15", "0"] -> [5, 15, 0]
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }

        return result;
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.ToString
    public static class BeylaSystemCheckResult {
        private final String kernelVersion;
        private final boolean btfSupported;
        private final boolean kernelVersionValid;
        private final String minimumKernelVersion;

        public boolean isEligible() {
            return kernelVersionValid && btfSupported;
        }
    }
}
