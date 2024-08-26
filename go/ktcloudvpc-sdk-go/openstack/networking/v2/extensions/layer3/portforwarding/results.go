package portforwarding

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

type PortForwarding struct {									// Modified
	// The ID of the floating IP port forwarding
	PortForwardingID 	string `json:"id"`
	PortForwardingName 	string `json:"name"`
	PrivateIP 			string `json:"vmguestip"`	
	PublicIP	  		string `json:"ipaddress"`
	PublicIpID	  		string `json:"ipaddressid"`
	// The IP protocol used in the floating IP port forwarding.
	Protocol 			string `json:"protocol"`
	ExternalPort      	string `json:"publicport"`
	ExternalEndPort   	string `json:"publicendport"`  // Caution!!) Spelling (Reqest struct : endpublicport)
	VpcID 				string `json:"vpcid"`
	SubnetID 			string `json:"networkid"`		// Tier ID
	InternalPort      	string `json:"privateport"`
	InternalEndPort   	string `json:"privateendport"` // Caution!!) Spelling (Reqest struct : endprivateport)	
}

type commonResult struct {
	gophercloud.Result
}

// CreateResult represents the result of a create operation. Call its Extract
// method to interpret it as a PortForwarding.
type CreateResult struct {
	commonResult
}

// GetResult represents the result of a get operation. Call its Extract
// method to interpret it as a PortForwarding.
type GetResult struct {
	commonResult
}

// UpdateResult represents the result of an update operation. Call its Extract
// method to interpret it as a PortForwarding.
type UpdateResult struct {
	commonResult
}

// DeleteResult represents the result of a delete operation. Call its
// ExtractErr method to determine if the request succeeded or failed.
type DeleteResult struct {
	gophercloud.ErrResult
}
// type DeleteResult struct {															// Modified
// 	commonResult
// }

// Extract will extract a Port Forwarding resource from a result.
func (r commonResult) Extract() (*PortForwarding, error) {
	var s PortForwarding
	err := r.ExtractInto(&s)
	return &s, err
}

// func (r commonResult) ExtractInto(v interface{}) error {
// 	return r.Result.ExtractIntoStructPtr(v, "job_id")
// }

type CreatePortforwardingResponse struct {											// Added
	JopID string `json:"job_id"`
}

func (r commonResult) ExtractJobInfo() (*CreatePortforwardingResponse, error) {   	// Added
	var s struct {
		CreatePortforwardingResponse *CreatePortforwardingResponse `json:"nc_createportforwardingruleresponse"`
	}
	err := r.ExtractInto(&s)
	return s.CreatePortforwardingResponse, err
}

// type DellPortforwardingResponse struct {											// Added
// 	JopID string `json:"job_id"`
// }

// func (r commonResult) ExtractDelJobInfo() (*DellPortforwardingResponse, error) {   	// Added
// 	var s struct {
// 		DellPortforwardingResponse *DellPortforwardingResponse `json:"nc_deleteportforwardingruleresponse"`
// 	}
// 	err := r.ExtractInto(&s)
// 	return s.DellPortforwardingResponse, err
// }

// PortForwardingPage is the page returned by a pager when traversing over a
// collection of port forwardings.
type PortForwardingPage struct {
	pagination.LinkedPageBase
}

// NextPageURL is invoked when a paginated collection of port forwardings has
// reached the end of a page and the pager seeks to traverse over a new one.
// In order to do this, it needs to construct the next page's URL.
func (r PortForwardingPage) NextPageURL() (string, error) {
	var s struct {
		Links []gophercloud.Link `json:"port_forwarding_links"`
	}
	err := r.ExtractInto(&s)
	if err != nil {
		return "", err
	}
	return gophercloud.ExtractNextURL(s.Links)
}

// IsEmpty checks whether a PortForwardingPage struct is empty.
func (r PortForwardingPage) IsEmpty() (bool, error) {
	is, err := ExtractPortForwardings(r)
	return len(is) == 0, err
}

type PFRules struct {											  // Added
	PortForwardings []PortForwarding `json:"portforwardingrule"`
}

// ExtractPortForwardings accepts a Page struct, specifically a PortForwardingPage
// struct, and extracts the elements into a slice of PortForwarding structs. In
// other words, a generic collection is mapped into a relevant slice.
func ExtractPortForwardings(r pagination.Page) ([]PortForwarding, error) {		// Modified
	var s struct {
		PFRule PFRules `json:"nc_listportforwardingrulesresponse"`
	}
	err := (r.(PortForwardingPage)).ExtractInto(&s)
	return s.PFRule.PortForwardings, err
}
