package networks

import (
	"encoding/json"
	"time"

	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/networking/v2/subnets"
)

type commonResult struct {
	gophercloud.Result
}

// Extract is a function that accepts a result and extracts a network resource.
func (r commonResult) Extract() (*Network, error) {
	var s Network
	err := r.ExtractInto(&s)
	return &s, err
}

func (r commonResult) ExtractInto(v interface{}) error {		// Modified
	return r.Result.ExtractIntoStructPtr(v, "vpcs")
}


// CreateResult represents the result of a create operation. Call its Extract
// method to interpret it as a Network.
type CreateResult struct {
	commonResult
}

// GetResult represents the result of a get operation. Call its Extract
// method to interpret it as a Network.
type GetResult struct {
	commonResult
}

// UpdateResult represents the result of an update operation. Call its Extract
// method to interpret it as a Network.
type UpdateResult struct {
	commonResult
}

// DeleteResult represents the result of a delete operation. Call its
// ExtractErr method to determine if the request succeeded or failed.
type DeleteResult struct {
	gophercloud.ErrResult
}

type StaticRoute struct {	// Added
	Default bool `json:"default"`

	// CIDR representing IP range for this VPC, based on IP version.	
	CIDR string `json:"cidr"`

	ID int `json:"id"`

	Gateway string `json:"gateway"`

	GatewayInterfaceName string `json:"gatewayinterfacename"`
}

type StaticRoutes struct {	// Added
	// UUID for the VPC
	VpcID string `json:"vpcid"`

	StaticRoutes []StaticRoute `json:"staticroutes"`
}

// Network represents, well, a network.
// KT Cloud D1 API guide : https://cloud.kt.com/docs/open-api-guide/d/computing/networking
type Network struct {								// Modified
	VpcOfferingID string `json:"vpcofferingid"`		// Added

	SesionCount string `json:"sessioncount"`		// Added
		
	// UUID for the network
	ID string `json:"id"`

	// Human-readable name for the network. Might not be unique.
	Name string `json:"name"`

	ZoneID string `json:"zoneid"`					// Added

	Vdom string `json:"vdom"`						// Added

	// Subnets associated with this network.
	Subnets []subnets.Subnet `json:"networks"`		// Caution!! // Modified

	StaticRoutes StaticRoutes `json:"staticroutes"`	// Added

	Account string `json:"account"`					// Added
	
	// CreatedAt contain ISO-8601 timestamps of when it was created.
	CreatedAt time.Time `json:"-"`	
}

func (r *Network) UnmarshalJSON(b []byte) error {	// Modified
	type tmp Network

	// Support for older neutron time format
	var s1 struct {
		tmp
		CreatedAt gophercloud.JSONRFC3339NoZ `json:"created"`
		// UpdatedAt gophercloud.JSONRFC3339NoZ `json:"updated_at"`
	}

	err := json.Unmarshal(b, &s1)
	if err == nil {
		*r = Network(s1.tmp)
		r.CreatedAt = time.Time(s1.CreatedAt)
		// r.UpdatedAt = time.Time(s1.UpdatedAt)

		return nil
	}

	// Support for newer neutron time format
	var s2 struct {
		tmp
		CreatedAt time.Time `json:"created_at"`
		// UpdatedAt time.Time `json:"updated_at"`
	}

	err = json.Unmarshal(b, &s2)
	if err != nil {
		return err
	}

	*r = Network(s2.tmp)
	r.CreatedAt = time.Time(s2.CreatedAt)
	// r.UpdatedAt = time.Time(s2.UpdatedAt)

	return nil
}

// NetworkPage is the page returned by a pager when traversing over a
// collection of networks.
type NetworkPage struct {
	pagination.LinkedPageBase
}

// NextPageURL is invoked when a paginated collection of networks has reached
// the end of a page and the pager seeks to traverse over a new one. In order
// to do this, it needs to construct the next page's URL.
func (r NetworkPage) NextPageURL() (string, error) {
	var s struct {
		Links []gophercloud.Link `json:"networks_links"`
	}
	err := r.ExtractInto(&s)
	if err != nil {
		return "", err
	}
	return gophercloud.ExtractNextURL(s.Links)
}

// IsEmpty checks whether a NetworkPage struct is empty.
func (r NetworkPage) IsEmpty() (bool, error) {						// Modified
	is, err := ExtractVPCs(r)
	return len(is) == 0, err
}

// ExtractNetworks accepts a Page struct, specifically a NetworkPage struct,
// and extracts the elements into a slice of Network structs. In other words,
// a generic collection is mapped into a relevant slice.
func ExtractNetworks(r pagination.Page) ([]Network, error) {
	var s []Network
	err := ExtractNetworksInto(r, &s)
	return s, err
}

func ExtractVPCs(r pagination.Page) ([]Network, error) {				// Added
	type ListVPCs struct {												// Added
		VPCs []Network `json:"vpcs"`
	}
	
	var s struct {
		ListVPC ListVPCs `json:"nc_listvpcsresponse"`
	}		
	err := (r.(NetworkPage)).ExtractInto(&s)
	return s.ListVPC.VPCs, err
}

func ExtractNetworksInto(r pagination.Page, v interface{}) error {		// Modified
	return r.(NetworkPage).Result.ExtractIntoSlicePtr(v, "vpcs")
}
