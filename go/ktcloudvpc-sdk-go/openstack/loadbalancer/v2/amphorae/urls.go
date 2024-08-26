package amphorae

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

const (
	rootPath     = "octavia"
	resourcePath = "amphorae"
	failoverPath = "failover"
)

func rootURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL(rootPath, resourcePath)
}

func resourceURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL(rootPath, resourcePath, id)
}

func failoverRootURL(c *gophercloud.ServiceClient, id string) string {
	return c.ServiceURL(rootPath, resourcePath, id, failoverPath)
}
