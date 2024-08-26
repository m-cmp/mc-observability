//go:build acceptance || networking || loadbalancer || pools
// +build acceptance networking loadbalancer pools

package v2

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/loadbalancer/v2/pools"
)

func TestPoolsList(t *testing.T) {
	client, err := clients.NewLoadBalancerV2Client()
	if err != nil {
		t.Fatalf("Unable to create a loadbalancer client: %v", err)
	}

	allPages, err := pools.List(client, nil).AllPages()
	if err != nil {
		t.Fatalf("Unable to list pools: %v", err)
	}

	allPools, err := pools.ExtractPools(allPages)
	if err != nil {
		t.Fatalf("Unable to extract pools: %v", err)
	}

	for _, pool := range allPools {
		tools.PrintResource(t, pool)
	}
}
