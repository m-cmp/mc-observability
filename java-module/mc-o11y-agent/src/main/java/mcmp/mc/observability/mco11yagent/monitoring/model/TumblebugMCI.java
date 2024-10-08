package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TumblebugMCI {

    @JsonProperty("configure_cloud_adaptive_network")
    private String configureCloudAdaptiveNetwork;

    @JsonProperty("description")
    private String description;

    @JsonProperty("id")
    private String id;

    @JsonProperty("install_mon_agent")
    private String installMonAgent;

    @JsonProperty("label")
    private String label;

    @JsonProperty("name")
    private String name;

    @JsonProperty("new_vm_list")
    private List<String> newVmList;

    @JsonProperty("placement_algo")
    private String placementAlgo;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_count")
    private StatusCount statusCount;

    @JsonProperty("system_label")
    private String systemLabel;

    @JsonProperty("system_message")
    private String systemMessage;

    @JsonProperty("target_action")
    private String targetAction;

    @JsonProperty("target_status")
    private String targetStatus;

    @JsonProperty("vm")
    private Vm[] vm;

    @Getter
    @Setter
    public static class StatusCount {
        @JsonProperty("count_creating")
        private Long countCreating;

        @JsonProperty("count_failed")
        private Long countFailed;

        @JsonProperty("count_rebooting")
        private Long countRebooting;

        @JsonProperty("count_resuming")
        private Long countResuming;

        @JsonProperty("count_running")
        private Long countRunning;

        @JsonProperty("count_suspended")
        private Long countSuspended;

        @JsonProperty("count_suspending")
        private Long countSuspending;

        @JsonProperty("count_terminated")
        private Long countTerminated;

        @JsonProperty("count_terminating")
        private Long countTerminating;

        @JsonProperty("count_total")
        private Long countTotal;

        @JsonProperty("count_undefined")
        private Long countUndefined;
    }

    @Getter
    @Setter
    public static class Vm {
        @JsonProperty("connection_config")
        private ConnectionConfig connectionConfig;

        @JsonProperty("connection_name")
        private String connectionName;

        @JsonProperty("created_time")
        private String createdTime;

        @JsonProperty("csp_view_vm_detail")
        private CspViewVmDetail cspViewVmDetail;

        @JsonProperty("data_disk_ids")
        private List<String> dataDiskIds;

        @JsonProperty("description")
        private String description;

        @JsonProperty("id")
        private String id;

        @JsonProperty("id_by_csp")
        private String idByCSP;

        @JsonProperty("uid")
        private String uid;

        @JsonProperty("csp_resource_name")
        private String cspResourceName;

        @JsonProperty("csp_resource_id")
        private String cspResourceId;

        @JsonProperty("image_id")
        private String imageId;

        @JsonProperty("label")
        private String label;

        @JsonProperty("location")
        private Location location;

        @JsonProperty("mon_agent_status")
        private String monAgentStatus;

        @JsonProperty("name")
        private String name;

        @JsonProperty("network_agent_status")
        private String networkAgentStatus;

        @JsonProperty("private_dns")
        private String privateDNS;

        @JsonProperty("private_ip")
        private String privateIP;

        @JsonProperty("public_dns")
        private String publicDNS;

        @JsonProperty("public_ip")
        private String publicIP;

        @JsonProperty("region")
        private Region region;

        @JsonProperty("root_device_name")
        private String rootDeviceName;

        @JsonProperty("root_disk_size")
        private String rootDiskSize;

        @JsonProperty("root_disk_type")
        private String rootDiskType;

        @JsonProperty("security_group_ids")
        private List<String> securityGroupIds;

        @JsonProperty("spec_id")
        private String specId;

        @JsonProperty("ssh_key_id")
        private String sshKeyId;

        @JsonProperty("ssh_port")
        private String sshPort;

        @JsonProperty("status")
        private String status;

        @JsonProperty("sub_group_id")
        private String subGroupId;

        @JsonProperty("subnet_id")
        private String subnetId;

        @JsonProperty("system_message")
        private String systemMessage;

        @JsonProperty("target_action")
        private String targetAction;

        @JsonProperty("target_status")
        private String targetStatus;

        @JsonProperty("v_net_id")
        private String vNetId;

        @JsonProperty("vm_user_name")
        private String vmUserName;

        @JsonProperty("vm_user_account")
        private String vmUserAccount;

        @JsonProperty("vm_user_password")
        private String vmUserPassword;
    }

    @Getter
    @Setter
    public static class ConnectionConfig {
        @JsonProperty("config_name")
        private String configName;

        @JsonProperty("credential_holder")
        private String credentialHolder;

        @JsonProperty("credential_name")
        private String credentialName;

        @JsonProperty("driver_name")
        private String driverName;

        @JsonProperty("provider_name")
        private String providerName;

        @JsonProperty("region_detail")
        private RegionDetail regionDetail;

        @JsonProperty("region_representative")
        private Boolean regionRepresentative;

        @JsonProperty("region_zone_info")
        private RegionZoneInfo regionZoneInfo;

        @JsonProperty("region_zone_info_name")
        private String regionZoneInfoName;

        @JsonProperty("verified")
        private Boolean verified;
    }

    @Getter
    @Setter
    public static class RegionDetail {
        @JsonProperty("description")
        private String description;

        @JsonProperty("location")
        private Location location;

        @JsonProperty("region_id")
        private String regionId;

        @JsonProperty("region_name")
        private String regionName;

        @JsonProperty("zones")
        private List<String> zones;
    }

    @Getter
    @Setter
    public static class Location {
        @JsonProperty("display")
        private String display;

        @JsonProperty("latitude")
        private Long latitude;

        @JsonProperty("longitude")
        private Long longitude;
    }

    @Getter
    @Setter
    public static class RegionZoneInfo {
        @JsonProperty("assigned_region")
        private String assignedRegion;

        @JsonProperty("assigned_zone")
        private String assignedZone;
    }

    @Getter
    @Setter
    public static class CspViewVmDetail {
        @JsonProperty("cspid")
        private String cspid;

        @JsonProperty("data_disk_iids")
        private List<IID> dataDiskIIDs;

        @JsonProperty("data_disk_names")
        private List<String> dataDiskNames;

        @JsonProperty("iid")
        private IID iid;

        @JsonProperty("image_iid")
        private IID imageIID;

        @JsonProperty("image_name")
        private String imageName;

        @JsonProperty("image_type")
        private String imageType;

        @JsonProperty("key_pair_iid")
        private IID keyPairIID;

        @JsonProperty("key_pair_name")
        private String keyPairName;

        @JsonProperty("key_value_list")
        private List<KeyValue> keyValueList;

        @JsonProperty("name")
        private String name;

        @JsonProperty("network_interface")
        private String networkInterface;

        @JsonProperty("private_dns")
        private String privateDNS;

        @JsonProperty("private_ip")
        private String privateIP;

        @JsonProperty("public_dns")
        private String publicDNS;

        @JsonProperty("public_ip")
        private String publicIP;

        @JsonProperty("region")
        private String region;

        @JsonProperty("root_device_name")
        private String rootDeviceName;

        @JsonProperty("root_disk_size")
        private String rootDiskSize;

        @JsonProperty("root_disk_type")
        private String rootDiskType;

        @JsonProperty("security_group_iids")
        private List<IID> securityGroupIIds;

        @JsonProperty("security_group_names")
        private List<String> securityGroupNames;

        @JsonProperty("sshaccess_point")
        private String sshaccessPoint;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("subnet_iid")
        private String subnetIID;

        @JsonProperty("subnet_name")
        private String subnetName;

        @JsonProperty("vmspec_name")
        private String vmspecName;

        @JsonProperty("vmuser_passwd")
        private String vmuserPasswd;

        @JsonProperty("vpc_iid")
        private IID vpcIID;

        @JsonProperty("vpcname")
        private String vpcname;
    }

    @Getter
    @Setter
    public static class Region {
        @JsonProperty("region")
        private String region;

        @JsonProperty("zone")
        private String zone;
    }

    @Getter
    @Setter
    public static class IID {
        @JsonProperty("name_id")
        private String nameId;

        @JsonProperty("system_id")
        private String systemId;
    }

    @Getter
    @Setter
    public static class KeyValue {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;
    }
}
