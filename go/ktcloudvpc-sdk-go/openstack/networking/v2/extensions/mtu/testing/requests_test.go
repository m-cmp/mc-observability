package testing

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/networking/v2/extensions/mtu"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/networking/v2/networks"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestListExternal(t *testing.T) {
	networkListOpts := networks.ListOpts{
		ID: "d32019d3-bc6e-4319-9c1d-6722fc136a22",
	}

	listOpts := mtu.ListOptsExt{
		ListOptsBuilder: networkListOpts,
		MTU:             1500,
	}

	actual, err := listOpts.ToNetworkListQuery()
	th.AssertNoErr(t, err)
	th.AssertEquals(t, ExpectedListOpts, actual)
}
