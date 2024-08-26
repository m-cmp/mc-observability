package portforwarding

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

type ListOptsBuilder interface {
	ToPortForwardingListQuery() (string, error)
}

// ListOpts allows the filtering and sorting of paginated collections through
// the API. Filtering is achieved by passing in struct field values that map to
// the port forwarding attributes you want to see returned. SortKey allows you to
// sort by a particular network attribute. SortDir sets the direction, and is
// either `asc' or `desc'. Marker and Limit are used for pagination.
type ListOpts struct {
	ID                string `q:"id"`
	InternalPortID    string `q:"internal_port_id"`
	ExternalPort      string `q:"external_port"`
	InternalIPAddress string `q:"internal_ip_address"`
	Protocol          string `q:"protocol"`
	InternalPort      string `q:"internal_port"`
	SortKey           string `q:"sort_key"`
	SortDir           string `q:"sort_dir"`
	Fields            string `q:"fields"`
	Limit             int    `q:"limit"`
	Marker            string `q:"marker"`
}

// ToPortForwardingListQuery formats a ListOpts into a query string.
func (opts ListOpts) ToPortForwardingListQuery() (string, error) {
	q, err := gophercloud.BuildQueryString(opts)
	return q.String(), err
}

// List returns a Pager which allows you to iterate over a collection of
// Port Forwarding resources. It accepts a ListOpts struct, which allows you to
// filter and sort the returned collection for greater efficiency.
func List(client *gophercloud.ServiceClient, opts ListOptsBuilder) pagination.Pager {  			// Modified
	url := portForwardingUrl(client)
	if opts != nil {
		query, err := opts.ToPortForwardingListQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query
	}
	return pagination.NewPager(client, url, func(r pagination.PageResult) pagination.Page {
		return PortForwardingPage{pagination.LinkedPageBase{PageResult: r}}
	})
}

// Get retrieves a particular port forwarding resource based on its unique ID.
func Get(client *gophercloud.ServiceClient, floatingIpId string, pfId string) (r GetResult) {	// Modified
	resp, err := client.Get(singlePortForwardingUrl(client, pfId), &r.Body, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// CreateOpts contains all the values needed to create a new port forwarding resource. All attributes are required.
type CreateOpts struct {																		// Modified
	ZoneID 	  		  	string `json:"zoneid"` 			 // Ex. KT Cloud D1 Platform => 'DX-M1'
	PrivateIpAddr 	  	string `json:"vmguestip"`        // Private IP address (allocated to the server(VM))
	PublicIpID 		  	string `json:"entpublicipid"`
	Protocol          	string `json:"protocol"`

	ExternalPort      	string `json:"publicport"`
	ExternalStartPort   string `json:"startpublicport"`  // Public IP Start Port number (equal to the 'publicport' value),
	ExternalEndPort   	string `json:"endpublicport"`

	InternalPort      	string `json:"privateport"`
	InternalStartPort   string `json:"startprivateport"` // Private IP Start Port number (equal to the 'privateport' value),
	InternalEndPort   	string `json:"endprivateport"`
}

// CreateOptsBuilder allows extensions to add additional parameters to the
// Create request.
type CreateOptsBuilder interface {
	ToPortForwardingCreateMap() (map[string]interface{}, error)
}

// ToPortForwardingCreateMap allows CreateOpts to satisfy the CreateOptsBuilder
// interface
func (opts CreateOpts) ToPortForwardingCreateMap() (map[string]interface{}, error) {			// Modified
	return gophercloud.BuildRequestBody(opts, "")	
}

// Create accepts a CreateOpts struct and uses the values provided to create a new port forwarding for an existing floating IP.
func Create(client *gophercloud.ServiceClient, opts CreateOptsBuilder) (r CreateResult) {		// Modified
	b, err := opts.ToPortForwardingCreateMap()
	if err != nil {
		r.Err = err
		return
	}
	resp, err := client.Post(portForwardingUrl(client), b, &r.Body, &gophercloud.RequestOpts{
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// UpdateOpts contains the values used when updating a port forwarding resource.
type UpdateOpts struct {
	ZoneId 	  		  	string `json:"zoneid"` 			 // Ex. KT Cloud D1 Platform => 'DX-M1'
	PrivateIpAddr 	  	string `json:"vmguestip"`        // Private IP address (allocated to the server(VM))
	PublicIpId 		  	string `json:"entpublicipid"`
	Protocol          	string `json:"protocol"`

	ExternalPort      	string `json:"publicport"`
	ExternalStartPort   string `json:"startpublicport"`  // Public IP Start Port number (equal to the 'publicport' value),
	ExternalEndPort   	string `json:"endpublicport"`

	InternalPort      	string `json:"privateport"`
	InternalStartPort   string `json:"startprivateport"` // Private IP Start Port number (equal to the 'privateport' value),
	InternalEndPort   	string `json:"endprivateport"`
}

// ToPortForwardingUpdateMap allows UpdateOpts to satisfy the UpdateOptsBuilder
// interface
func (opts UpdateOpts) ToPortForwardingUpdateMap() (map[string]interface{}, error) {
	b, err := gophercloud.BuildRequestBody(opts, "port_forwarding")
	if err != nil {
		return nil, err
	}
	return b, nil
}

// UpdateOptsBuilder allows extensions to add additional parameters to the
// Update request.
type UpdateOptsBuilder interface {
	ToPortForwardingUpdateMap() (map[string]interface{}, error)
}

// Update allows port forwarding resources to be updated.
func Update(client *gophercloud.ServiceClient, pfID string, opts UpdateOptsBuilder) (r UpdateResult) {		// Modified
	b, err := opts.ToPortForwardingUpdateMap()
	if err != nil {
		r.Err = err
		return
	}
	resp, err := client.Put(singlePortForwardingUrl(client, pfID), b, &r.Body, &gophercloud.RequestOpts{
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// Delete will permanently delete a particular port forwarding for a given floating ID.
func Delete(client *gophercloud.ServiceClient, pfId string) (r DeleteResult) {								// Modified
	resp, err := client.Delete(singlePortForwardingUrl(client, pfId), &gophercloud.RequestOpts{
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}
