package tokens

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func tokenURL(c *gophercloud.ServiceClient) string {
	return c.ServiceURL("auth", "tokens")
}
