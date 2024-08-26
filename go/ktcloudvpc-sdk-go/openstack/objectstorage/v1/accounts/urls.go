package accounts

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func getURL(c *gophercloud.ServiceClient) string {
	return c.Endpoint
}

func updateURL(c *gophercloud.ServiceClient) string {
	return getURL(c)
}
