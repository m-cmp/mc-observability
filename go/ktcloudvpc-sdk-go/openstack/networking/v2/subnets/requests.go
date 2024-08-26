package subnets

import (
	"fmt"
	"github.com/sirupsen/logrus"
	cblog "github.com/cloud-barista/cb-log"

	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

var cblogger *logrus.Logger

func init() {
	cblogger = cblog.GetLogger("KTCloud VPC Client")
}

// ListOptsBuilder allows extensions to add additional parameters to the
// List request.
type ListOptsBuilder interface {
	ToSubnetListQuery() (string, error)
}

// ListOpts allows the filtering and sorting of paginated collections through
// the API. Filtering is achieved by passing in struct field values that map to
// the subnet attributes you want to see returned. SortKey allows you to sort
// by a particular subnet attribute. SortDir sets the direction, and is either
// `asc' or `desc'. Marker and Limit are used for pagination.
type ListOpts struct {
	Name            string `q:"name"`
	Description     string `q:"description"`
	EnableDHCP      *bool  `q:"enable_dhcp"`
	NetworkID       string `q:"network_id"`
	TenantID        string `q:"tenant_id"`
	ProjectID       string `q:"project_id"`
	IPVersion       int    `q:"ip_version"`
	GatewayIP       string `q:"gateway_ip"`
	CIDR            string `q:"cidr"`
	IPv6AddressMode string `q:"ipv6_address_mode"`
	IPv6RAMode      string `q:"ipv6_ra_mode"`
	ID              string `q:"id"`
	SubnetPoolID    string `q:"subnetpool_id"`
	Limit           int    `q:"limit"`
	Marker          string `q:"marker"`
	SortKey         string `q:"sort_key"`
	SortDir         string `q:"sort_dir"`
	Tags            string `q:"tags"`
	TagsAny         string `q:"tags-any"`
	NotTags         string `q:"not-tags"`
	NotTagsAny      string `q:"not-tags-any"`
}

// ToSubnetListQuery formats a ListOpts into a query string.
func (opts ListOpts) ToSubnetListQuery() (string, error) {
	q, err := gophercloud.BuildQueryString(opts)
	return q.String(), err
}

// List returns a Pager which allows you to iterate over a collection of
// subnets. It accepts a ListOpts struct, which allows you to filter and sort
// the returned collection for greater efficiency.
//
// Default policy settings return only those subnets that are owned by the tenant
// who submits the request, unless the request is submitted by a user with
// administrative rights.
func List(c *gophercloud.ServiceClient, opts ListOptsBuilder) pagination.Pager {
	url := listURL(c)
	cblogger.Infof("\n\n### Subnets list URL : %s", url)

	if opts != nil {
		query, err := opts.ToSubnetListQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query
	}
	return pagination.NewPager(c, url, func(r pagination.PageResult) pagination.Page {
		return SubnetPage{pagination.LinkedPageBase{PageResult: r}}
	})
}

// Get retrieves a specific subnet based on its unique ID.
func Get(c *gophercloud.ServiceClient, id string) (r GetResult) {
	cblogger.Infof("\n\n### Subnet getURL(c, id) : %s", getURL(c, id))

	resp, err := c.Get(getURL(c, id), &r.Body, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// CreateOptsBuilder allows extensions to add additional parameters to the
// List request.
type CreateOptsBuilder interface {
	ToSubnetCreateMap() (map[string]interface{}, error)
}

type DetailInfo struct {							// Added. Tier 상세 설정
	Cclass 		string `json:"cclass,omitempty"`			// Added

	CIDR 		string `json:"cidr,omitempty"`				// Added

	StartIP 	string `json:"startip,omitempty"`			// Added. Server가 사용 가능한 첫번째 ip

	EndIP 		string `json:"endip,omitempty"`			    // Added. Server가 사용 가능한 마지막 ip

	LBStartIP 	string `json:"lbstartip,omitempty"`			// Added. LoadBalancer가 사용 가능한 첫번째 ip

	LBEndIP 	string `json:"lbendip,omitempty"`			// Added. LoadBalancer가 사용 가능한 마지막 ip

	BMStartIP 	string `json:"bmstartip,omitempty"`			// Added. Bare Metal 또는 기타 목적으로 사용 가능한 첫번째 ip

	BMEndIP 	string `json:"bmendip,omitempty"`			// Added.  또는 기타 목적으로 사용 가능한 마지막 ip

	Gateway 	string `json:"gateway,omitempty"`			// Added
}

// CreateOpts represents the attributes used when creating a new subnet.
type CreateOpts struct {								// Modified
	// Name is a human-readable name of the subnet.
	Name 		string `json:"name" required:"true"`

	Zone 		string `json:"zone" required:"true"`		// Added

	Type 		string `json:"type" required:"true"`		// Added. Network Creation type. 기본값으로 "tier"만 사용 가능

	UserCustom 	string `json:"usercustom" required:"true"`	// Added.
	// 사용자가 tier 생성 정보를 수동으로 설정?. Default : "n"
		// "n" : C class만 사용
		// "y" : C class 미사용 및 그 외 필드 사용

	Detail DetailInfo `json:"detail,omitempty"`		    	// Added. Tier 상세 설정	
}

// ToSubnetCreateMap builds a request body from CreateOpts.
func (opts CreateOpts) ToSubnetCreateMap() (map[string]interface{}, error) {
	b, err := gophercloud.BuildRequestBody(opts, "")  // Caution!!
	if err != nil {
		return nil, err
	}
	return b, nil
}
/*
b, err := gophercloud.BuildRequestBody(opts, "subnet") 
==> 
# body :
(map[string]interface {}) (len=1) {
	(string) (len=6) "subnet": (map[string]interface {}) (len=5) {
		(string) (len=4) "name": (string) (len=9) "ktsubnet3",
		(string) (len=4) "zone": (string) (len=5) "DX-M1",
		(string) (len=4) "type": (string) (len=4) "tier",
		(string) (len=10) "usercustom": (string) (len=1) "y",
		(string) (len=6) "detail": (map[string]interface {}) (len=8) {
			(string) (len=7) "bmendip": (string) (len=12) "172.25.7.250",
			(string) (len=7) "gateway": (string) (len=10) "172.25.7.1",
			(string) (len=4) "cidr": (string) (len=13) "172.25.7.0/24",
			(string) (len=7) "startip": (string) (len=11) "172.25.7.11",
			(string) (len=5) "endip": (string) (len=12) "172.25.7.120",
			(string) (len=9) "lbstartip": (string) (len=12) "172.25.7.140",
			(string) (len=7) "lbendip": (string) (len=12) "172.25.7.190",
			(string) (len=9) "bmstartip": (string) (len=12) "172.25.7.201"
		}
	}
}

b, err := gophercloud.BuildRequestBody(opts, "")
==>
# body :
(map[string]interface {}) (len=5) {
	(string) (len=4) "name": (string) (len=9) "ktsubnet3",
	(string) (len=4) "zone": (string) (len=5) "DX-M1",
	(string) (len=4) "type": (string) (len=4) "tier",
	(string) (len=10) "usercustom": (string) (len=1) "y",
	(string) (len=6) "detail": (map[string]interface {}) (len=8) {
		(string) (len=7) "bmendip": (string) (len=12) "172.25.7.250",
		(string) (len=7) "gateway": (string) (len=10) "172.25.7.1",
		(string) (len=4) "cidr": (string) (len=13) "172.25.7.0/24",
		(string) (len=7) "startip": (string) (len=11) "172.25.7.11",
		(string) (len=5) "endip": (string) (len=12) "172.25.7.120",
		(string) (len=9) "lbstartip": (string) (len=12) "172.25.7.140",
		(string) (len=7) "lbendip": (string) (len=12) "172.25.7.190",
		(string) (len=9) "bmstartip": (string) (len=12) "172.25.7.201"
	}
}
*/

// Create accepts a CreateOpts struct and creates a new subnet using the values
// provided. You must remember to provide a valid NetworkID, CIDR and IP
// version.
func Create(c *gophercloud.ServiceClient, opts CreateOptsBuilder) (r CreateResult) {
	b, err := opts.ToSubnetCreateMap()
	if err != nil {
		r.Err = err
		return
	}
	resp, err := c.Post(createURL(c), b, &r.Body, &gophercloud.RequestOpts{			// Modified
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// UpdateOptsBuilder allows extensions to add additional parameters to the
// Update request.
type UpdateOptsBuilder interface {
	ToSubnetUpdateMap() (map[string]interface{}, error)
}

// UpdateOpts represents the attributes used when updating an existing subnet.
type UpdateOpts struct {
	// Name is a human-readable name of the subnet.
	Name *string `json:"name,omitempty"`

	// Description of the subnet.
	Description *string `json:"description,omitempty"`

	// AllocationPools are IP Address pools that will be available for DHCP.
	AllocationPools []AllocationPool `json:"allocation_pools,omitempty"`

	// GatewayIP sets gateway information for the subnet. Setting to nil will
	// cause a default gateway to automatically be created. Setting to an empty
	// string will cause the subnet to be created with no gateway. Setting to
	// an explicit address will set that address as the gateway.
	GatewayIP *string `json:"gateway_ip,omitempty"`

	// DNSNameservers are the nameservers to be set via DHCP.
	DNSNameservers *[]string `json:"dns_nameservers,omitempty"`

	// HostRoutes are any static host routes to be set via DHCP.
	HostRoutes *[]HostRoute `json:"host_routes,omitempty"`

	// EnableDHCP will either enable to disable the DHCP service.
	EnableDHCP *bool `json:"enable_dhcp,omitempty"`

	// RevisionNumber implements extension:standard-attr-revisions. If != "" it
	// will set revision_number=%s. If the revision number does not match, the
	// update will fail.
	RevisionNumber *int `json:"-" h:"If-Match"`
}

// ToSubnetUpdateMap builds a request body from UpdateOpts.
func (opts UpdateOpts) ToSubnetUpdateMap() (map[string]interface{}, error) {
	b, err := gophercloud.BuildRequestBody(opts, "subnet")
	if err != nil {
		return nil, err
	}
	return b, nil
}

// Update accepts a UpdateOpts struct and updates an existing subnet using the
// values provided.
func Update(c *gophercloud.ServiceClient, id string, opts UpdateOptsBuilder) (r UpdateResult) {
	b, err := opts.ToSubnetUpdateMap()
	if err != nil {
		r.Err = err
		return
	}
	h, err := gophercloud.BuildHeaders(opts)
	if err != nil {
		r.Err = err
		return
	}
	for k := range h {
		if k == "If-Match" {
			h[k] = fmt.Sprintf("revision_number=%s", h[k])
		}
	}

	resp, err := c.Put(updateURL(c, id), b, &r.Body, &gophercloud.RequestOpts{
		MoreHeaders: h,
		OkCodes:     []int{200, 201},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// Delete accepts a unique ID and deletes the subnet associated with it.
func Delete(c *gophercloud.ServiceClient, id string) (r DeleteResult) {
	resp, err := c.Delete(deleteURL(c, id), &gophercloud.RequestOpts{			// Modified
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}
