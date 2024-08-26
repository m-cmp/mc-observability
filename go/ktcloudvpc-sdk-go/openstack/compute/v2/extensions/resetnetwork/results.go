package resetnetwork

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

// ResetResult is the response of a ResetNetwork operation. Call its ExtractErr
// method to determine if the request suceeded or failed.
type ResetResult struct {
	gophercloud.ErrResult
}
