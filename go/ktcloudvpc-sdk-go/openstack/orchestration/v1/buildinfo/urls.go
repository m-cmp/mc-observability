package buildinfo

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func getURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("build_info")
}
