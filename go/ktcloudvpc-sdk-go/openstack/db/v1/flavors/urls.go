package flavors

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func getURL(client *gophercloud.ServiceClient, id string) string {
	return client.ServiceURL("flavors", id)
}

func listURL(client *gophercloud.ServiceClient) string {
	return client.ServiceURL("flavors")
}
