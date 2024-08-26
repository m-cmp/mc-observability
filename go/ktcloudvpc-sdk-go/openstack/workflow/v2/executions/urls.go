package executions

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func createURL(client *gophercloud.ServiceClient) string {
	return client.ServiceURL("executions")
}

func getURL(client *gophercloud.ServiceClient, id string) string {
	return client.ServiceURL("executions", id)
}

func deleteURL(client *gophercloud.ServiceClient, id string) string {
	return client.ServiceURL("executions", id)
}

func listURL(client *gophercloud.ServiceClient) string {
	return client.ServiceURL("executions")
}
