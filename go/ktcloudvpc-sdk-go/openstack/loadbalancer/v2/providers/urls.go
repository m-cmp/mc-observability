package providers

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

const (
	rootPath     = "lbaas"
	resourcePath = "providers"
)

func rootURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL(rootPath, resourcePath)
}
