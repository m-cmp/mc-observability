package availabilityzones

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func listURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("os-availability-zone")
}
