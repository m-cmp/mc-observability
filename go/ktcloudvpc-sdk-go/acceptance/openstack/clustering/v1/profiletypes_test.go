//go:build acceptance || clustering || profiletypes
// +build acceptance clustering profiletypes

package v1

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/clustering/v1/profiletypes"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestProfileTypesList(t *testing.T) {
	client, err := clients.NewClusteringV1Client()
	th.AssertNoErr(t, err)

	client.Microversion = "1.5"

	allPages, err := profiletypes.List(client).AllPages()
	th.AssertNoErr(t, err)

	allProfileTypes, err := profiletypes.ExtractProfileTypes(allPages)
	th.AssertNoErr(t, err)

	for _, profileType := range allProfileTypes {
		tools.PrintResource(t, profileType)
	}
}
func TestProfileTypesOpsList(t *testing.T) {
	client, err := clients.NewClusteringV1Client()
	th.AssertNoErr(t, err)

	client.Microversion = "1.5"

	profileTypeName := "os.nova.server-1.0"
	allPages, err := profiletypes.ListOps(client, profileTypeName).AllPages()
	th.AssertNoErr(t, err)

	ops, err := profiletypes.ExtractOps(allPages)
	th.AssertNoErr(t, err)

	for k, v := range ops {
		tools.PrintResource(t, k)
		tools.PrintResource(t, v)
	}
}
