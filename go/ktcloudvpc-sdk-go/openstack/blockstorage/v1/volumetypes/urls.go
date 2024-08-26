package volumetypes

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func listURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("types")
}

func createURL(c *gophercloud.ServiceClient) string {
	return listURL(c)
}

func getURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("types", id)
}

func deleteURL(c *gophercloud.ServiceClient, id string) string {
	return getURL(c, id)
}
