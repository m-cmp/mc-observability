package com.mcmp.o11ymanager.manager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.exception.vm.VMAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.repository.VMJpaRepository;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VMServiceImplTest {

    @Mock private VMJpaRepository vmJpaRepository;
    @Mock private RequestInfo requestInfo;

    @InjectMocks private VMServiceImpl vmService;

    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";

    private VMEntity createVMEntity(VMAgentTaskStatus traceStatus) {
        return VMEntity.builder()
                .nsId(NS_ID)
                .mciId(MCI_ID)
                .vmId(VM_ID)
                .name("test-vm")
                .influxSeq(1L)
                .traceAgentTaskStatus(traceStatus)
                .monitoringAgentTaskStatus(VMAgentTaskStatus.IDLE)
                .logAgentTaskStatus(VMAgentTaskStatus.IDLE)
                .build();
    }

    @Nested
    @DisplayName("isIdleTraceAgent()")
    class IsIdleTraceAgentTests {

        @Test
        @DisplayName("IDLE 상태 -> 예외 없음")
        void idle_noException() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.IDLE);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));

            assertThatNoException()
                    .isThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID));
        }

        @Test
        @DisplayName("NULL 상태 -> 예외 없음 (기존 레코드 호환)")
        void nullStatus_noException() {
            VMEntity vm = createVMEntity(null);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));

            assertThatNoException()
                    .isThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID));
        }

        @Test
        @DisplayName("INSTALLING 상태 -> VMAgentTaskProcessingException")
        void installing_throwsException() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.INSTALLING);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(requestInfo.getRequestId()).thenReturn("req-123");

            assertThatThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(VMAgentTaskProcessingException.class);
        }

        @Test
        @DisplayName("UPDATING 상태 -> VMAgentTaskProcessingException")
        void updating_throwsException() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.UPDATING);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(requestInfo.getRequestId()).thenReturn("req-123");

            assertThatThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(VMAgentTaskProcessingException.class);
        }

        @Test
        @DisplayName("FINISHED 상태 -> VMAgentTaskProcessingException (IDLE이 아니므로)")
        void finished_throwsException() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.FINISHED);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(requestInfo.getRequestId()).thenReturn("req-123");

            assertThatThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(VMAgentTaskProcessingException.class);
        }

        @Test
        @DisplayName("VM이 존재하지 않으면 ResourceNotExistsException")
        void vmNotFound_throwsException() {
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.empty());
            when(requestInfo.getRequestId()).thenReturn("req-123");

            assertThatThrownBy(() -> vmService.isIdleTraceAgent(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(ResourceNotExistsException.class);
        }
    }

    @Nested
    @DisplayName("updateTraceAgentTaskStatus()")
    class UpdateTraceAgentTaskStatusTests {

        @Test
        @DisplayName("IDLE로 설정 시 taskId를 빈문자로 초기화")
        void setIdle_clearsTaskId() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.INSTALLING);
            vm.setVmTraceAgentTaskId("42");
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(vmJpaRepository.save(any(VMEntity.class))).thenReturn(vm);

            vmService.updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.IDLE);

            assertThat(vm.getTraceAgentTaskStatus()).isEqualTo(VMAgentTaskStatus.IDLE);
            assertThat(vm.getVmTraceAgentTaskId()).isEmpty();
            verify(vmJpaRepository).save(vm);
        }

        @Test
        @DisplayName("INSTALLING으로 설정 시 taskId 유지")
        void setInstalling_keepsTaskId() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.IDLE);
            vm.setVmTraceAgentTaskId("42");
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(vmJpaRepository.save(any(VMEntity.class))).thenReturn(vm);

            vmService.updateTraceAgentTaskStatus(
                    NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.INSTALLING);

            assertThat(vm.getTraceAgentTaskStatus()).isEqualTo(VMAgentTaskStatus.INSTALLING);
            assertThat(vm.getVmTraceAgentTaskId()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("updateTraceAgentTaskStatusAndTaskId()")
    class UpdateTraceAgentTaskStatusAndTaskIdTests {

        @Test
        @DisplayName("상태와 taskId를 동시에 업데이트")
        void updatesStatusAndTaskId() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.IDLE);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));
            when(vmJpaRepository.save(any(VMEntity.class))).thenReturn(vm);

            vmService.updateTraceAgentTaskStatusAndTaskId(
                    NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.INSTALLING, "99");

            assertThat(vm.getTraceAgentTaskStatus()).isEqualTo(VMAgentTaskStatus.INSTALLING);
            assertThat(vm.getVmTraceAgentTaskId()).isEqualTo("99");
            verify(vmJpaRepository).save(vm);
        }
    }

    @Nested
    @DisplayName("getHostLock()")
    class GetHostLockTests {

        @Test
        @DisplayName("동일 키 -> 같은 Lock 인스턴스")
        void sameKey_sameLock() {
            ReentrantLock lock1 = vmService.getHostLock(NS_ID, MCI_ID, VM_ID);
            ReentrantLock lock2 = vmService.getHostLock(NS_ID, MCI_ID, VM_ID);

            assertThat(lock1).isSameAs(lock2);
        }

        @Test
        @DisplayName("다른 키 -> 다른 Lock 인스턴스")
        void differentKey_differentLock() {
            ReentrantLock lock1 = vmService.getHostLock(NS_ID, MCI_ID, VM_ID);
            ReentrantLock lock2 = vmService.getHostLock(NS_ID, MCI_ID, "vm-2");

            assertThat(lock1).isNotSameAs(lock2);
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("존재하지 않는 VM 조회 시 ResourceNotExistsException")
        void notFound_throwsException() {
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.empty());
            when(requestInfo.getRequestId()).thenReturn("req-123");

            assertThatThrownBy(() -> vmService.get(NS_ID, MCI_ID, VM_ID))
                    .isInstanceOf(ResourceNotExistsException.class);
        }

        @Test
        @DisplayName("존재하는 VM 조회 시 VMDTO 반환")
        void found_returnsDTO() {
            VMEntity vm = createVMEntity(VMAgentTaskStatus.IDLE);
            when(vmJpaRepository.findByNsIdAndMciIdAndVmId(NS_ID, MCI_ID, VM_ID))
                    .thenReturn(Optional.of(vm));

            var result = vmService.get(NS_ID, MCI_ID, VM_ID);

            assertThat(result.getVmId()).isEqualTo(VM_ID);
            assertThat(result.getName()).isEqualTo("test-vm");
        }
    }
}
