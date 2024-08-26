package catalog

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func listURL(client *gophercloud.ServiceClient) string {
	return client.ServiceURL("auth", "catalog")
}
