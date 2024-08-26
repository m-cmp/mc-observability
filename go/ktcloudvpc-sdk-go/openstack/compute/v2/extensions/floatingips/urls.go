package floatingips

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

const resourcePath = "IpAddress"  						// Modified

func resourceURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL(resourcePath)
}

func listURL(c *gophercloud.ServiceClient) string {
	return resourceURL(c)
}

func createURL(c *gophercloud.ServiceClient) string {
	return resourceURL(c)
}

func getURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL(resourcePath, id)
}

func deleteURL(c *gophercloud.ServiceClient, id string) string {
	return getURL(c, id)
}

func serverURL(c *gophercloud.ServiceClient, serverID string) string {
	return c.ServiceURL("servers/" + serverID + "/action")
}

func associateURL(c *gophercloud.ServiceClient, serverID string) string {
	return serverURL(c, serverID)
}

func disassociateURL(c *gophercloud.ServiceClient, serverID string) string {
	return serverURL(c, serverID)
}
