//go:build acceptance || imageservice || imageimport
// +build acceptance imageservice imageimport

package v2

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestGetImportInfo(t *testing.T) {
	client, err := clients.NewImageServiceV2Client()
	th.AssertNoErr(t, err)

	importInfo, err := GetImportInfo(t, client)
	th.AssertNoErr(t, err)

	tools.PrintResource(t, importInfo)
}

func TestCreateImport(t *testing.T) {
	client, err := clients.NewImageServiceV2Client()
	th.AssertNoErr(t, err)

	image, err := CreateEmptyImage(t, client)
	th.AssertNoErr(t, err)
	defer DeleteImage(t, client, image)

	err = ImportImage(t, client, image.ID)
	th.AssertNoErr(t, err)
}
