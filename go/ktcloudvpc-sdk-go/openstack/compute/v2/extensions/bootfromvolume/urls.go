package bootfromvolume

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func createURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("servers")
}
