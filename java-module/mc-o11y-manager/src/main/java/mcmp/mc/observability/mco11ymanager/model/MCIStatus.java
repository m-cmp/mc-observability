package mcmp.mc.observability.mco11ymanager.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MCIStatus {
    List<MCI> mci;

    @Getter
    @Setter
    public static class MCI {
        private String id;
        private String installMonAgent;
        private String label;
        private String masterIp;
        private String masterSSHPort;
        private String masterVmId;
        private String name;
        private String status;
        private StatusCount statusCount;
        private String systemLabel;
        private String targetAction;
        private String targetStatus;
        private List<Vm> vm;
    }

    @Getter
    @Setter
    public static class StatusCount {
        private Long countCreating;
        private Long countFailed;
        private Long countRebooting;
        private Long countResuming;
        private Long countRunning;
        private Long countSuspended;
        private Long countSuspending;
        private Long countTerminated;
        private Long countTerminating;
        private Long countTotal;
        private Long countUndefined;
    }

    @Getter
    @Setter
    public static class Vm {
        private String createdTime;
        private String cspVmId;
        private String id;
        private Location location;
        private String monAgentStatus;
        private String name;
        private String nativeStatus;
        private String privateIp;
        private String publicIp;
        private String sshPort;
        private String status;
        private String systemMessage;
        private String targetAction;
        private String targetStatus;
    }

    @Getter
    @Setter
    public static class Location {
        private String display;
        private Long latitude;
        private Long longitude;
    }
}