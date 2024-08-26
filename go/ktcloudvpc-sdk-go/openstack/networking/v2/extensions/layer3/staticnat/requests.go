package staticnat

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

type ListOptsBuilder interface {
	ToStaticNatListQuery() (string, error)
}

// ListOpts allows the filtering and sorting of paginated collections through
// the API. Filtering is achieved by passing in struct field values that map to
// the Static NAT attributes you want to see returned.
type ListOpts struct {
}

// ToStaticNatListQuery formats a ListOpts into a query string.
func (opts ListOpts) ToStaticNatListQuery() (string, error) {
	q, err := gophercloud.BuildQueryString(opts)
	return q.String(), err
}

// List returns a Pager which allows you to iterate over a collection of
// Static NAT resources. It accepts a ListOpts struct, which allows you to
// filter and sort the returned collection for greater efficiency.
func List(client *gophercloud.ServiceClient, opts ListOptsBuilder) pagination.Pager {  			// Modified
	url := staticNatUrl(client)
	if opts != nil {
		query, err := opts.ToStaticNatListQuery()
		if err != nil {
			return pagination.Pager{Err: err}
		}
		url += query
	}
	return pagination.NewPager(client, url, func(r pagination.PageResult) pagination.Page {
		return StaticNatPage{pagination.LinkedPageBase{PageResult: r}}
	})
}

// Get retrieves a particular Static NAT resource based on its unique ID.
func Get(client *gophercloud.ServiceClient, floatingIpId string, pfId string) (r GetResult) {	// Modified
	resp, err := client.Get(singleStaticNatUrl(client, pfId), &r.Body, nil)
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// CreateOpts contains all the values needed to create a new Static NAT resource. All attributes are required.
type CreateOpts struct {																		// Added
	PrivateIpAddr 	  	string `json:"vmguestip"`       // Private IP address (allocated to the server(VM))
	SubnetID 		  	string `json:"vmnetworkid"`		// ID of the Tier the VM belongs to
	PublicIpID 		  	string `json:"entpublicipid"`	// Public IP ID to set up Static NAT
}

// CreateOptsBuilder allows extensions to add additional parameters to the
// Create request.
type CreateOptsBuilder interface {
	ToStaticNatCreateMap() (map[string]interface{}, error)
}

// ToStaticNatCreateMap allows CreateOpts to satisfy the CreateOptsBuilder
// interface
func (opts CreateOpts) ToStaticNatCreateMap() (map[string]interface{}, error) {			// Added
	return gophercloud.BuildRequestBody(opts, "")	
}

// Create accepts a CreateOpts struct and uses the values provided to create a new Static NAT for an existing floating IP.
func Create(client *gophercloud.ServiceClient, opts CreateOptsBuilder) (r CreateResult) {		// Added
	b, err := opts.ToStaticNatCreateMap()
	if err != nil {
		r.Err = err
		return
	}
	resp, err := client.Post(staticNatUrl(client), b, &r.Body, &gophercloud.RequestOpts{
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}

// Delete will permanently delete a particular Static NAT for a given floating ID.
func Delete(client *gophercloud.ServiceClient, pfId string) (r DeleteResult) {					// Added
	resp, err := client.Delete(singleStaticNatUrl(client, pfId), &gophercloud.RequestOpts{
		OkCodes: []int{200},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}
