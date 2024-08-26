//go:build acceptance || compute || availabilityzones
// +build acceptance compute availabilityzones

package v2

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/compute/v2/extensions/availabilityzones"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestAvailabilityZonesList(t *testing.T) {
	client, err := clients.NewComputeV2Client()
	th.AssertNoErr(t, err)

	allPages, err := availabilityzones.List(client).AllPages()
	th.AssertNoErr(t, err)

	availabilityZoneInfo, err := availabilityzones.ExtractAvailabilityZones(allPages)
	th.AssertNoErr(t, err)

	var found bool
	for _, zoneInfo := range availabilityZoneInfo {
		tools.PrintResource(t, zoneInfo)

		if zoneInfo.ZoneName == "nova" {
			found = true
		}
	}

	th.AssertEquals(t, found, true)
}

func TestAvailabilityZonesListDetail(t *testing.T) {
	clients.RequireAdmin(t)

	client, err := clients.NewComputeV2Client()
	th.AssertNoErr(t, err)

	allPages, err := availabilityzones.ListDetail(client).AllPages()
	th.AssertNoErr(t, err)

	availabilityZoneInfo, err := availabilityzones.ExtractAvailabilityZones(allPages)
	th.AssertNoErr(t, err)

	var found bool
	for _, zoneInfo := range availabilityZoneInfo {
		tools.PrintResource(t, zoneInfo)

		if zoneInfo.ZoneName == "nova" {
			found = true
		}
	}

	th.AssertEquals(t, found, true)
}
