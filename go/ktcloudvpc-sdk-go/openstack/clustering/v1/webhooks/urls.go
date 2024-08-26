package webhooks

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

func triggerURL(client *gophercloud.ServiceClient, id string) string {
	return client.ServiceURL("v1", "webhooks", id, "trigger")
}
