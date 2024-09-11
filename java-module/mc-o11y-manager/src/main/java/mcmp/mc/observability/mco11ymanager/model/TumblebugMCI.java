package mcmp.mc.observability.mco11ymanager.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TumblebugMCI {
    private String configureCloudAdaptiveNetwork;
    private String description;
    private String id;
    private String installMonAgent;
    private String label;
    private String name;
    private List<String> newVmList;
    private String placementAlgo;
    private String status;
    private StatusCount statusCount;
    private String systemLabel;
    private String systemMessage;
    private String targetAction;
    private String targetStatus;
    private Vm[] vm;

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
        private ConnectionConfig connectionConfig;
        private String connectionName;
        private String createdTime;
        private CspViewVmDetail cspViewVmDetail;
        private List<String> dataDiskIds;
        private String description;
        private String id;
        private String idByCSP;
        private String imageId;
        private String label;
        private Location location;
        private String monAgentStatus;
        private String name;
        private String networkAgentStatus;
        private String privateDNS;
        private String privateIP;
        private String publicDNS;
        private String publicIP;
        private Region region;
        private String rootDeviceName;
        private String rootDiskSize;
        private String rootDiskType;
        private List<String> securityGroupIds;
        private String specId;
        private String sshKeyId;
        private String sshPort;
        private String status;
        private String subGroupId;
        private String subnetId;
        private String systemMessage;
        private String targetAction;
        private String targetStatus;
        private String vNetId;
        private String vmUserName;
        private String vmUserAccount;
        private String vmUserPassword;
    }

    @Getter
    @Setter
    public static class ConnectionConfig {
        private String configName;
        private String credentialHolder;
        private String credentialName;
        private String driverName;
        private String providerName;
        private RegionDetail regionDetail;
        private Boolean regionRepresentative;
        private RegionZoneInfo regionZoneInfo;
        private String regionZoneInfoName;
        private Boolean verified;
    }

    @Getter
    @Setter
    public static class RegionDetail {
        private String description;
        private Location location;
        private String regionId;
        private String regionName;
        private List<String> zones;
    }

    @Getter
    @Setter
    public static class Location {
        private String display;
        private Long latitude;
        private Long longitude;
    }

    @Getter
    @Setter
    public static class RegionZoneInfo {
        private String assignedRegion;
        private String assignedZone;
    }

    @Getter
    @Setter
    public static class CspViewVmDetail {
        private String cspid;
        private List<IID> dataDiskIIDs;
        private List<String> dataDiskNames;
        private String iid;
        private IID imageIID;
        private String imageName;
        private String imageType;
        private IID keyPairIID;
        private String keyPairName;
        private List<KeyValue> keyValueList;
        private String name;
        private String networkInterface;
        private String privateDNS;
        private String privateIP;
        private String publicDNS;
        private String publicIP;
        private String region;
        private String rootDeviceName;
        private String rootDiskSize;
        private String rootDiskType;
        private List<IID> securityGroupIIds;
        private List<String> securityGroupNames;
        private String sshaccessPoint;
        private String startTime;
        private String subnetIID;
        private String subnetName;
        private String vmspecName;
        private String vmuserPasswd;
        private IID vpcIID;
        private String vpcname;

    }

    @Getter
    @Setter
    public static class Region {
        private String region;
        private String zone;
    }

    @Getter
    @Setter
    public static class IID {
        private String nameId;
        private String systemId;
    }

    @Getter
    @Setter
    public static class KeyValue {
        private String key;
        private String value;
    }
}
