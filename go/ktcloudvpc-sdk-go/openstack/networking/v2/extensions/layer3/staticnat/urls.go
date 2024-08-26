package staticnat

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

const resourcePath = "StaticNat"									// Added

func staticNatUrl(c *gophercloud.ServiceClient) string {			// Added
	return c.ServiceURL(resourcePath)
}

func singleStaticNatUrl(c *gophercloud.ServiceClient, staticNatID string) string {	// Added
	return c.ServiceURL(resourcePath, staticNatID)
}
