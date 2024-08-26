package subnets

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func resourceURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("Network", id)  // Modified
}

func rootURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("Network")  // Modified
}

func listURL(c *gophercloud.ServiceClient) string {
	return rootURL(c)
}

func getURL(c *gophercloud.ServiceClient, id string) string {
	return resourceURL(c, id)
}

func createURL(c *gophercloud.ServiceClient) string {
	return rootURL(c)
}

func updateURL(c *gophercloud.ServiceClient, id string) string {
	return resourceURL(c, id)
}

func deleteURL(c *gophercloud.ServiceClient, id string) string {
	return resourceURL(c, id)
}
