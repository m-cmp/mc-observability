package extensions

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func ActionURL(client *gophercloud.ServiceClient, id string) string {
	return client.ServiceURL("servers", id, "action")
}
