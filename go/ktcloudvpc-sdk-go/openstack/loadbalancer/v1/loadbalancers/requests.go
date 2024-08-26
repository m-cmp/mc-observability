package loadbalancers

import (
	"fmt"
	// "net/url"
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	// "github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/loadbalancer/v2/listeners"
	// "github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/loadbalancer/v2/pools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

// ListOptsBuilder allows extensions to add additional parameters to the
// List request.
type ListOptsBuilder interface {
	ToLoadBalancerListQuery() (string, error)
}

// ListOpts allows the filtering and sorting of paginated collections through
// the API. Filtering is achieved by passing in struct field values that map to
// the Loadbalancer attributes you want to see returned. SortKey allows you to
// sort by a particular attribute. SortDir sets the direction, and is
// either `asc' or `desc'. Marker and Limit are used for pagination.
type ListOpts struct {
	Description        string   `q:"description"`
	AdminStateUp       *bool    `q:"admin_state_up"`
	ProjectID          string   `q:"project_id"`
	ProvisioningStatus string   `q:"provisioning_status"`
	VipAddress         string   `q:"vip_address"`
	VipPortID          string   `q:"vip_port_id"`
	VipSubnetID        string   `q:"vip_subnet_id"`
	VipNetworkID       string   `q:"vip_network_id"`
	
	OperatingStatus    string   `q:"operating_status"`
	Name               string   `q:"name"`
	FlavorID           string   `q:"flavor_id"`
	AvailabilityZone   string   `q:"availability_zone"`
	Provider           string   `q:"provider"`
	Limit              int      `q:"limit"`
	Marker             string   `q:"marker"`
	SortKey            string   `q:"sort_key"`
	SortDir            string   `q:"sort_dir"`
	Tags               []string `q:"tags"`
	TagsAny            []string `q:"tags-any"`
	TagsNot            []string `q:"not-tags"`
	TagsNotAny         []string `q:"not-tags-any"`

	ZoneID				string   `q:"zoneid"`
	NlbID               string   `q:"loadbalancerid"`	
}

// ToLoadBalancerListQuery formats a ListOpts into a query string.
func (opts ListOpts) ToLoadBalancerListQuery() (string, error) { 	// Modified
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	// q, err := gophercloud.BuildQueryString(opts)	
	// return q.String(), err
	return q, err
}

// List returns a Pager which allows you to iterate over a collection of
// load balancers. It accepts a ListOpts struct, which allows you to filter
// and sort the returned collection for greater efficiency.
//
// Default policy settings return only those load balancers that are owned by
// the project who submits the request, unless an admin user submits the request.
func List(c *gophercloud.ServiceClient, opts ListOptsBuilder) pagination.Pager { 	// Modified
	url := listNlbURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerListQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query		
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	return pagination.NewPager(c, url, func(r pagination.PageResult) pagination.Page {
		return LoadBalancerPage{pagination.LinkedPageBase{PageResult: r}}
	})
}


// CreateOptsBuilder allows extensions to add additional parameters to the
// Create request.
type CreateOptsBuilder interface {
	ToLoadBalancerCreateQuery() (string, error)
}

// CreateOpts is the common options struct used in this package's Create
// operation.
type CreateOpts struct {											// Modified
	Name             string `q:"name"`				// Required
	ZoneID           string `q:"zoneid"`				// Required. Zone ID that has the 'ServiceIP'
	NlbOption 		 string `q:"loadbalanceroption"`	// Required. roundrobin / leastconnection / leastresponse / sourceiphash / srcipsrcporthash
	ServiceIP        string `q:"serviceip"`			// Required. KT Cloud Virtual IP. 
														// 'ServiceIP' : $$$ In case of an empty value(""), it is newly created.
	ServicePort      string `q:"serviceport"`		// Required
	ServiceType      string `q:"servicetype"`		// Required. NLB ServiceType : https / http / sslbridge / tcp / ftp
	HealthCheckType  string `q:"healthchecktype"`	// Required. HealthCheckType : http / https / tcp
	HealthCheckURL   string `q:"healthcheckurl"`		// Required. URL when the HealthCheckType is 'http' or 'https'.
	CipherGroupName  string `q:"ciphergroupname"`	// Required when ServiceType is 'https'. Set CipherGroup Name
	SSLv3        	 string `q:"sslv3"`				// Required when ServiceType is 'https'. Use SSLv3? : 'DISABLED' / 'ENABLED'
	TLSv1        	 string `q:"tlsv1"`				// Required when ServiceType is 'https'. Use TLSv1? : 'DISABLED' / 'ENABLED'
	TLSv11         	 string `q:"tlsv11"`				// Required when ServiceType is 'https'. Use TLSv11? : 'DISABLED' / 'ENABLED'
	TLSv12        	 string `q:"tlsv12"`				// Required when ServiceType is 'https'. Use TLSv12? : 'DISABLED' / 'ENABLED'
	NetworkID        string `q:"networkid"` 			// Tier Network ID. Required in case of 'Enterprise Security'
}
// ### To create query string : Not `json:"name"` But `q:"name"`

// ToLoadBalancerListQuery formats a ListOpts into a query string.
func (opts CreateOpts) ToLoadBalancerCreateQuery() (string, error) { 	// Addeed
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	// q, err := gophercloud.BuildQueryString(opts)	
	// return q.String(), err
	return q, err
}

/*
func Create(c *gophercloud.ServiceClient, opts CreateOptsBuilder) pagination.Pager { 	// Modified
	url := createNlbURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerCreateQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query		
	}
	
	// url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	return pagination.NewPager(c, url, func(r pagination.PageResult) pagination.Page {
		return LoadBalancerPage{pagination.LinkedPageBase{PageResult: r}}
	})
}
*/

func Create(c *gophercloud.ServiceClient, opts CreateOptsBuilder) (r CreateResult)  { 	// Modified
	url := createNlbURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerCreateQuery()
		if err != nil {		
			r.commonResult.Result.Err = err		// Modified
			return r
		}
		url += query
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	resp, err := c.Get(url, &r.Body, nil) // Caution!!
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// ToLoadBalancerCreateMap builds a request body from CreateOpts.
func (opts CreateOpts) ToLoadBalancerCreateMap() (map[string]interface{}, error) {
	return gophercloud.BuildRequestBody(opts, "loadbalancer")
}

// DeleteOptsBuilder allows extensions to add additional parameters to the
// Delete request.
type DeleteOptsBuilder interface {
	ToLoadBalancerDeleteQuery() (string, error)
}

// DeleteOpts is the common options struct used in this package's Delete
// operation.
type DeleteOpts struct {
	NlbID            string   `q:"loadbalancerid"`	
}

// ToLoadBalancerDeleteQuery formats a DeleteOpts into a query string.
func (opts DeleteOpts) ToLoadBalancerDeleteQuery() (string, error) { // Modified
	q, err := gophercloud.BuildGetMethodQueryString(opts)
	return q, err
}

// Delete will permanently delete a particular LoadBalancer based on its
// unique ID.
func Delete(c *gophercloud.ServiceClient, opts DeleteOptsBuilder) (r DeleteResult) { // Modified
	url := deleteNlbURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerDeleteQuery()
		if err != nil {		
			r.ErrResult.Err = err
			return r
		}
		url += query
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	resp, err := c.Get(url, &r.Body, nil) // Caution!!) Not c.Delete(url, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// GetStatuses will return the status of a particular LoadBalancer.
func GetStatuses(c *gophercloud.ServiceClient, id string) (r GetStatusesResult) {
	resp, err := c.Get(statusRootURL(c, id), &r.Body, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// GetStats will return the shows the current statistics of a particular LoadBalancer.
func GetStats(c *gophercloud.ServiceClient, id string) (r StatsResult) {
	resp, err := c.Get(statisticsRootURL(c, id), &r.Body, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// Failover performs a failover of a load balancer.
func Failover(c *gophercloud.ServiceClient, id string) (r FailoverResult) {
	resp, err := c.Put(failoverRootURL(c, id), nil, nil, &gophercloud.RequestOpts{
		OkCodes: []int{202},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// Add Load Balancer Server
type AddServerOpts struct {											// Addied
	NlbID 		 string `q:"loadbalancerid"`	// Required
	VMID 		 string `q:"virtualmachineid"`	// Required
	IPAddress    string `q:"ipaddress"`			// Required.	
	PublicPort   string `q:"publicport"`		// Required
}

type AddServerOptsBuilder interface {
	ToLoadBalancerAddServerQuery() (string, error)
}

func AddServer(c *gophercloud.ServiceClient, opts AddServerOptsBuilder) (r AddServerResult)  { 	// Added
	url := addServerURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerAddServerQuery()
		if err != nil {		
			r.commonResult.Result.Err = err		// Modified
			return r
		}
		url += query
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	resp, err := c.Get(url, &r.Body, nil) // Caution!!
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

func (opts AddServerOpts) ToLoadBalancerAddServerQuery() (string, error) { 	// Addeed
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	return q, err
}

// Remove Load Balancer Server
type RemoveServerOpts struct {											// Added
	ServiceID 		 string `q:"serviceid"`	// Required
}

type RemoveServerOptsBuilder interface {
	ToLoadBalancerRemoveServerQuery() (string, error)
}

func RemoveServer(c *gophercloud.ServiceClient, opts RemoveServerOptsBuilder) (r RemoveServerResult)  { 	// Added
	url := removeServerURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerRemoveServerQuery()
		if err != nil {		
			r.commonResult.Result.Err = err		// Modified
			return r
		}
		url += query
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	resp, err := c.Get(url, &r.Body, nil) // Caution!!
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

func (opts RemoveServerOpts) ToLoadBalancerRemoveServerQuery() (string, error) { 	// Addeed
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	return q, err
}

// List Load Balancer
type ListLbServerOpts struct {
	NlbID               string   `q:"loadbalancerid"`	
}

type ListLbServerOptsBuilder interface {
	ToLbServerListQuery() (string, error)
}

func (opts ListOpts) ToLbServerListQuery() (string, error) { 	// Modified
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	// q, err := gophercloud.BuildQueryString(opts)	
	// return q.String(), err
	return q, err
}

func ListLbServer(c *gophercloud.ServiceClient, opts ListLbServerOptsBuilder) pagination.Pager { 	// Modified
	url := listLbServerURL(c)
	if opts != nil {
		query, err := opts.ToLbServerListQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query		
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	return pagination.NewPager(c, url, func(r pagination.PageResult) pagination.Page {
		return LoadBalancerPage{pagination.LinkedPageBase{PageResult: r}}
	})
}

// Create Tag for Load Balancer
type CreateTagOpts struct {											// Addied
	NlbID 		 string `q:"loadbalancerid"`	// Required
	Tag 		 string `q:"tag"`				// Required
}

type CreateTagOptsBuilder interface {
	ToLoadBalancerCreateTagQuery() (string, error)
}

func CreateTag(c *gophercloud.ServiceClient, opts CreateTagOptsBuilder) (r CreateTagResult)  { 	// Added
	url := createTagURL(c)
	if opts != nil {
		query, err := opts.ToLoadBalancerCreateTagQuery()
		if err != nil {		
			r.commonResult.Result.Err = err		// Modified
			return r
		}
		url += query
	}
	
	url = url + "&response=json"
	fmt.Printf("\n### Call URL : %s\n\n", url)

	resp, err := c.Get(url, &r.Body, nil) // Caution!!
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

func (opts CreateTagOpts) ToLoadBalancerCreateTagQuery() (string, error) { 	// Addeed
	q, err := gophercloud.BuildGetMethodQueryString(opts)    		// # BuildGetMethodQueryString() method Created
	return q, err
}
