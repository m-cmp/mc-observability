package snapshots

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func createURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("snapshots")
}

func listDetailURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("snapshots", "detail")
}

func deleteURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("snapshots", id)
}

func getURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("snapshots", id)
}

func updateURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL("snapshots", id)
}
