package base

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func getURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL()
}

func pingURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("ping")
}
