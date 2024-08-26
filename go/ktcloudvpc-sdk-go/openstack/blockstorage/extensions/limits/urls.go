package limits

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

const resourcePath = "limits"

func getURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL(resourcePath)
}
