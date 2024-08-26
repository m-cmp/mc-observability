package rules

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

type Rule struct {											// Added
    Acls      []Acl  `json:"acls"`
    VpcID     string `json:"vpcid"`
}

type Acl struct {											// Added
    SrcIntfs  []NetworkInterface `json:"srcintfs"`
    Schedule  string             `json:"schedule"`
    Comments  string             `json:"comments"`
    NatIP     string             `json:"natip"`
    DstAddrs  []Address          `json:"dstaddrs"`
    Name      string             `json:"name"`
    DstIntfs  []NetworkInterface `json:"dstintfs"`
    Action    string             `json:"action"`
    ID        int                `json:"id"`
    Services  []Service          `json:"services"`
    SrcAddrs  []Address          `json:"srcaddrs"`
    Status    string             `json:"status"`
}

type NetworkInterface struct {								// Added
    NetworkName string `json:"networkname"`
    NetworkID   string `json:"networkid"`
    Interface   string `json:"interface"`
}

type Address struct {										// Added
    IP string `json:"ip"`
}

type Service struct {										// Added
    StartPort string `json:"startport"`
    Protocol  string `json:"protocol"`
    EndPort   string `json:"endport"`
}

// RulePage is the page returned by a pager when traversing over a
// collection of firewall rules.
type RulePage struct {
	pagination.LinkedPageBase
}

// NextPageURL is invoked when a paginated collection of firewall rules has
// reached the end of a page and the pager seeks to traverse over a new one.
// In order to do this, it needs to construct the next page's URL.
func (r RulePage) NextPageURL() (string, error) {
	var s struct {
		Links []gophercloud.Link `json:"firewall_rules_links"`
	}
	err := r.ExtractInto(&s)
	if err != nil {
		return "", err
	}
	return gophercloud.ExtractNextURL(s.Links)
}

// IsEmpty checks whether a RulePage struct is empty.
func (r RulePage) IsEmpty() (bool, error) {
	is, err := ExtractRules(r)
	return len(is) == 0, err
}

type FirewallRules struct {											    // Added
	Rules []Rule `json:"firewallrules"`
}

// ExtractRules accepts a Page struct, specifically a RouterPage struct,
// and extracts the elements into a slice of Router structs. In other words,
// a generic collection is mapped into a relevant slice.
func ExtractRules(r pagination.Page) ([]Rule, error) {					// Modified
	var s struct {
		RuleList FirewallRules `json:"nc_listfirewallrulesresponse"`
	}
	err := (r.(RulePage)).ExtractInto(&s)
	return s.RuleList.Rules, err
}

type commonResult struct {
	gophercloud.Result
}

// Extract is a function that accepts a result and extracts a firewall rule.
func (r commonResult) Extract() (*Rule, error) {
	var s struct {
		Rule *Rule `json:"firewall_rule"`
	}
	err := r.ExtractInto(&s)
	return s.Rule, err
}

type CreateFirewallRuleResponse struct {											// Added
	JopID string `json:"job_id"`
}

func (r commonResult) ExtractJobInfo() (*CreateFirewallRuleResponse, error) {   	// Added
	var s struct {
		CreateFirewallRuleResponse *CreateFirewallRuleResponse `json:"nc_createfirewallruleresponse"`
	}
	err := r.ExtractInto(&s)
	return s.CreateFirewallRuleResponse, err
}

// type DellFirewallRuleResponse struct {												// Added
// 	JopID string `json:"job_id"`
// }

// func (r commonResult) ExtractDelJobInfo() (*DellFirewallRuleResponse, error) {   	// Added
// 	var s struct {
// 		DellFirewallRuleResponse *DellFirewallRuleResponse `json:"nc_deletefirewallruleresponse"`
// 	}
// 	err := r.ExtractInto(&s)
// 	return s.DellFirewallRuleResponse, err
// }

// GetResult represents the result of a get operation.
type GetResult struct {
	commonResult
}

// UpdateResult represents the result of an update operation.
type UpdateResult struct {
	commonResult
}

// DeleteResult represents the result of a delete operation.
type DeleteResult struct {
	gophercloud.ErrResult
}
// type DeleteResult struct {															// Modified
// 	commonResult
// }

// CreateResult represents the result of a create operation.
type CreateResult struct {
	commonResult
}
