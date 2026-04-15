package com.mcmp.o11ymanager.manager.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mcmp.o11ymanager.manager.exception.agent.BeylaSystemRequirementException;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator.BeylaSystemCheckResult;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BeylaSystemRequirementValidatorTest {

    @Mock private TumblebugService tumblebugService;

    @InjectMocks private BeylaSystemRequirementValidator validator;

    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";

    private void mockKernelVersion(String version) {
        when(tumblebugService.executeCommand(eq(NS_ID), eq(MCI_ID), eq(VM_ID), eq("uname -r")))
                .thenReturn(version);
    }

    private void mockBtfSupport(String result) {
        when(tumblebugService.executeCommand(
                        eq(NS_ID),
                        eq(MCI_ID),
                        eq(VM_ID),
                        eq("test -f /sys/kernel/btf/vmlinux && echo 'true' || echo 'false'")))
                .thenReturn(result);
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("커널 5.15 + BTF 지원 -> eligible")
        void kernel5_15_btfTrue_eligible() {
            mockKernelVersion("5.15.0-91-generic");
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isTrue();
            assertThat(result.getKernelVersion()).isEqualTo("5.15.0-91-generic");
            assertThat(result.isBtfSupported()).isTrue();
            assertThat(result.isKernelVersionValid()).isTrue();
            assertThat(result.getMinimumKernelVersion()).isEqualTo("5.8.0");
        }

        @Test
        @DisplayName("커널 5.8.0 정확히 경계값 -> eligible")
        void kernel5_8_0_exact_boundary_eligible() {
            mockKernelVersion("5.8.0");
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isTrue();
            assertThat(result.isKernelVersionValid()).isTrue();
        }

        @Test
        @DisplayName("커널 4.18 + BTF 지원 -> not eligible (커널 미달)")
        void kernel4_18_btfTrue_notEligible() {
            mockKernelVersion("4.18.0-305.el8.x86_64");
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isFalse();
            assertThat(result.isKernelVersionValid()).isFalse();
            assertThat(result.isBtfSupported()).isTrue();
        }

        @Test
        @DisplayName("커널 5.15 + BTF 미지원 -> not eligible")
        void kernel5_15_btfFalse_notEligible() {
            mockKernelVersion("5.15.0");
            mockBtfSupport("false");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isFalse();
            assertThat(result.isKernelVersionValid()).isTrue();
            assertThat(result.isBtfSupported()).isFalse();
        }

        @Test
        @DisplayName("커널 4.x + BTF 미지원 -> 둘 다 미달")
        void kernel4_btfFalse_notEligible() {
            mockKernelVersion("4.15.0");
            mockBtfSupport("false");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isFalse();
            assertThat(result.isKernelVersionValid()).isFalse();
            assertThat(result.isBtfSupported()).isFalse();
        }

        @Test
        @DisplayName("uname -r 실패 시 unknown 반환, kernel invalid")
        void unameFails_unknown() {
            when(tumblebugService.executeCommand(
                            eq(NS_ID), eq(MCI_ID), eq(VM_ID), eq("uname -r")))
                    .thenThrow(new RuntimeException("SSH connection failed"));
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.getKernelVersion()).isEqualTo("unknown");
            assertThat(result.isKernelVersionValid()).isFalse();
            assertThat(result.isEligible()).isFalse();
        }

        @Test
        @DisplayName("BTF 체크 실패 시 false 반환")
        void btfCheckFails_false() {
            mockKernelVersion("5.15.0");
            when(tumblebugService.executeCommand(
                            eq(NS_ID),
                            eq(MCI_ID),
                            eq(VM_ID),
                            eq("test -f /sys/kernel/btf/vmlinux && echo 'true' || echo 'false'")))
                    .thenThrow(new RuntimeException("SSH timeout"));

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isBtfSupported()).isFalse();
        }

        @Test
        @DisplayName("uname -r 결과에 공백/개행 포함 시 trim 처리")
        void kernelVersion_withWhitespace_trimmed() {
            mockKernelVersion("  5.15.0-91-generic\n");
            mockBtfSupport("  true\n");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isEligible()).isTrue();
            assertThat(result.getKernelVersion()).isEqualTo("5.15.0-91-generic");
        }

        @Test
        @DisplayName("uname -r 결과가 null인 경우 unknown")
        void kernelVersion_null_unknown() {
            mockKernelVersion(null);
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.getKernelVersion()).isEqualTo("unknown");
            assertThat(result.isKernelVersionValid()).isFalse();
        }

        @Test
        @DisplayName("커널 5.7.9 -> 미달 (5.8.0 미만)")
        void kernel5_7_9_notEligible() {
            mockKernelVersion("5.7.9");
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isKernelVersionValid()).isFalse();
        }

        @Test
        @DisplayName("커널 6.1.0 -> eligible (5.8.0 초과)")
        void kernel6_1_eligible() {
            mockKernelVersion("6.1.0");
            mockBtfSupport("true");

            BeylaSystemCheckResult result = validator.validate(NS_ID, MCI_ID, VM_ID);

            assertThat(result.isKernelVersionValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateAndThrow()")
    class ValidateAndThrowTests {

        @Test
        @DisplayName("eligible이면 예외 없음")
        void eligible_noException() {
            mockKernelVersion("5.15.0");
            mockBtfSupport("true");

            assertThatNoException()
                    .isThrownBy(() -> validator.validateAndThrow(NS_ID, MCI_ID, VM_ID));
        }

        @Test
        @DisplayName("커널 버전 미달 시 BeylaSystemRequirementException")
        void kernelInvalid_throwsException() {
            mockKernelVersion("4.18.0");
            mockBtfSupport("true");

            assertThatThrownBy(() -> validator.validateAndThrow(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(BeylaSystemRequirementException.class)
                    .hasMessageContaining("Kernel version");
        }

        @Test
        @DisplayName("BTF 미지원 시 BeylaSystemRequirementException")
        void btfNotSupported_throwsException() {
            mockKernelVersion("5.15.0");
            mockBtfSupport("false");

            assertThatThrownBy(() -> validator.validateAndThrow(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(BeylaSystemRequirementException.class)
                    .hasMessageContaining("BTF");
        }
    }

    @Nested
    @DisplayName("parseKernelVersion() - private method")
    class ParseKernelVersionTests {

        private int[] invokeParseKernelVersion(String version) {
            return ReflectionTestUtils.invokeMethod(validator, "parseKernelVersion", version);
        }

        @Test
        @DisplayName("표준 3자리 버전 파싱")
        void standard_threePartVersion() {
            int[] result = invokeParseKernelVersion("5.15.0");
            assertThat(result).containsExactly(5, 15, 0);
        }

        @Test
        @DisplayName("suffix가 포함된 버전 파싱")
        void versionWithSuffix() {
            int[] result = invokeParseKernelVersion("5.15.0-91-generic");
            assertThat(result).containsExactly(5, 15, 0);
        }

        @Test
        @DisplayName("2자리 버전 파싱 (나머지 0)")
        void twoPartVersion() {
            int[] result = invokeParseKernelVersion("5.8");
            assertThat(result).containsExactly(5, 8, 0);
        }

        @Test
        @DisplayName("+ suffix 제거")
        void plusSuffix() {
            int[] result = invokeParseKernelVersion("5.10.0+custom");
            assertThat(result).containsExactly(5, 10, 0);
        }
    }
}
