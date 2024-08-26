package networks

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func resourceURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("VPC", id)  // Modified
}

func rootURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("VPC")  // Modified
}

func getURL(c *gophercloud.ServiceClient, id string) string {
	return resourceURL(c, id)
}

func listURL(c *gophercloud.ServiceClient) string {
	return rootURL(c)
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
