package apiversions

import (
	"strings"

	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/utils"
)

func listURL(c *gophercloud.ServiceClient) string {
	baseEndpoint, _ := utils.BaseEndpoint(c.Endpoint)
	endpoint := strings.TrimRight(baseEndpoint, "/") + "/"
	return endpoint
}
