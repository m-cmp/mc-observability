package injectnetworkinfo

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

// InjectNetworkResult is the response of a InjectNetworkInfo operation. Call
// its ExtractErr method to determine if the request suceeded or failed.
type InjectNetworkResult struct {
	gophercloud.ErrResult
}
