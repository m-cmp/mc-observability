package testing

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/baremetal/apiversions"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper/client"
)

func TestListAPIVersions(t *testing.T) {
	th.SetupHTTP()
	defer th.TeardownHTTP()

	MockListResponse(t)

	actual, err := apiversions.List(client.ServiceClient()).Extract()
	th.AssertNoErr(t, err)

	th.AssertDeepEquals(t, IronicAllAPIVersionResults, *actual)
}

func TestGetAPIVersion(t *testing.T) {
	th.SetupHTTP()
	defer th.TeardownHTTP()

	MockGetResponse(t)

	actual, err := apiversions.Get(client.ServiceClient(), "v1").Extract()
	th.AssertNoErr(t, err)

	th.AssertDeepEquals(t, IronicAPIVersion1Result, *actual)
}
