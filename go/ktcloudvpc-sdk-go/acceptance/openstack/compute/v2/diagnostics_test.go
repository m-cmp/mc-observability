//go:build acceptance || compute || limits
// +build acceptance compute limits

package v2

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/compute/v2/extensions/diagnostics"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestDiagnostics(t *testing.T) {
	clients.RequireAdmin(t)

	client, err := clients.NewComputeV2Client()
	th.AssertNoErr(t, err)

	server, err := CreateServer(t, client)
	th.AssertNoErr(t, err)
	defer DeleteServer(t, client, server)

	diag, err := diagnostics.Get(client, server.ID).Extract()
	th.AssertNoErr(t, err)

	tools.PrintResource(t, diag)

	_, ok := diag["memory"]
	th.AssertEquals(t, true, ok)
}
