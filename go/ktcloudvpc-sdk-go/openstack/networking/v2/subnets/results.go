package subnets

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

type commonResult struct {
	gophercloud.Result
}

// Extract is a function that accepts a result and extracts a subnet resource.
func (r commonResult) Extract() (*Subnet, error) {				// Modified
	var s struct {
		Subnet *Subnet `json:"network"`  // Caution!!
	}
	err := r.ExtractInto(&s)
	return s.Subnet, err
}

// CreateResult represents the result of a create operation. Call its Extract
// method to interpret it as a Subnet.
type CreateResult struct {
	commonResult
}

// GetResult represents the result of a get operation. Call its Extract
// method to interpret it as a Subnet.
type GetResult struct {
	commonResult
}

// UpdateResult represents the result of an update operation. Call its Extract
// method to interpret it as a Subnet.
type UpdateResult struct {
	commonResult
}

// DeleteResult represents the result of a delete operation. Call its
// ExtractErr method to determine if the request succeeded or failed.
type DeleteResult struct {
	gophercloud.ErrResult
}

// AllocationPool represents a sub-range of cidr available for dynamic
// allocation to ports, e.g. {Start: "10.0.0.2", End: "10.0.0.254"}
type AllocationPool struct {
	Start string `json:"start"`
	End   string `json:"end"`
}

// HostRoute represents a route that should be used by devices with IPs from
// a subnet (not including local subnet route).
type HostRoute struct {
	DestinationCIDR string `json:"destination"`
	NextHop         string `json:"nexthop"`
}

// Subnet represents a subnet. See package documentation for a top-level
// description of what this is.
// KT Cloud D1 API guide : https://cloud.kt.com/docs/open-api-guide/d/computing/tier
type Subnet struct {									// Modified
	EndIP 		string `json:"endip"`						// Added

	Shared 		string `json:"shared"`						// Added

	StartIP 	string `json:"startip"`						// Added
	
	Type 		string `json:"type"`						// Added

	VLan 		string `json:"vlan"`						// Added

	Netmask 	string `json:"netmask"`						// Added

	// UUID of the parent network.
	VpcID 		string `json:"vpcid"` 						// Modified

	// Human-readable name for the subnet. Might not be unique.
	Name 		string `json:"name"`

	ZoneID 		string `json:"zoneid"`						// Added
	
	DataLakeYN 	string `json:"datalakeyn"`					// Added
	
	// CIDR representing IP range for this subnet, based on IP version.
	CIDR 		string `json:"cidr"`

	// UUID representing the subnet.
	ID 			string `json:"id"`

	// ProjectID is the project owner of the subnet.
	ProjectID 	string `json:"projectid"`					// Modified

	// Default gateway used by devices in this subnet.
	Gateway 	string `json:"gateway"`						// Modified

	Account 	string `json:"account"`						// Added

	OsName 		string `json:"osname"`						// Added

	OsNetworkID string `json:"osnetworkid"`					// Added

	Status 		string `json:"status"`						// Added
}

// SubnetPage is the page returned by a pager when traversing over a collection
// of subnets.
type SubnetPage struct {
	pagination.LinkedPageBase
}

// NextPageURL is invoked when a paginated collection of subnets has reached
// the end of a page and the pager seeks to traverse over a new one. In order
// to do this, it needs to construct the next page's URL.
func (r SubnetPage) NextPageURL() (string, error) {
	var s struct {
		Links []gophercloud.Link `json:"subnets_links"`
	}
	err := r.ExtractInto(&s)
	if err != nil {
		return "", err
	}
	return gophercloud.ExtractNextURL(s.Links)
}

// IsEmpty checks whether a SubnetPage struct is empty.
func (r SubnetPage) IsEmpty() (bool, error) {
	is, err := ExtractSubnets(r)
	return len(is) == 0, err
}

type OsNetwork struct {												// Added
	Subnets []Subnet `json:"networks"`  // Caution!!
}

// ExtractSubnets accepts a Page struct, specifically a SubnetPage struct,
// and extracts the elements into a slice of Subnet structs. In other words,
// a generic collection is mapped into a relevant slice.
func ExtractSubnets(r pagination.Page) ([]Subnet, error) {			// Modified
	var s struct {
		OsNet OsNetwork `json:"nc_listosnetworksresponse"`
	}		
	err := (r.(SubnetPage)).ExtractInto(&s)
	return s.OsNet.Subnets, err
}

type CreateSubnetResponse struct {
		NetworkID string 		`json:"network_id"`
		VLAN      string 		`json:"vlan"`
		Success   interface{}  	`json:"success"`
} 
// In case CIDR is 172.25.x.x/24 => KT API returns success : string type
// In case CIDR is 10.25.x.x/24  => KT API returns success : bool type

func (r commonResult) ExtractCreateInfo() (*CreateSubnetResponse, error) {			// Added
	var s struct {
		SubnetCreateInfo *CreateSubnetResponse `json:"nc_createosnetworkresponse"`
	}
	err := r.ExtractInto(&s)
	return s.SubnetCreateInfo, err
}
