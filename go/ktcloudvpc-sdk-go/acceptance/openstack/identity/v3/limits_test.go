//go:build acceptance
// +build acceptance

package v3

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/identity/v3/limits"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestLimitsList(t *testing.T) {
	clients.RequireAdmin(t)

	client, err := clients.NewIdentityV3Client()
	th.AssertNoErr(t, err)

	listOpts := limits.ListOpts{}

	allPages, err := limits.List(client, listOpts).AllPages()
	th.AssertNoErr(t, err)

	_, err = limits.ExtractLimits(allPages)
	th.AssertNoErr(t, err)
}
