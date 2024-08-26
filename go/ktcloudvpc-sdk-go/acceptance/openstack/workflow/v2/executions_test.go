package v2

import (
	"testing"

	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/clients"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/acceptance/tools"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/workflow/v2/executions"
	th "github.com/cloud-barista/ktcloudvpc-sdk-go/testhelper"
)

func TestExecutionsCreate(t *testing.T) {
	client, err := clients.NewWorkflowV2Client()
	th.AssertNoErr(t, err)

	workflow, err := CreateWorkflow(t, client)
	th.AssertNoErr(t, err)
	defer DeleteWorkflow(t, client, workflow)

	execution, err := CreateExecution(t, client, workflow)
	th.AssertNoErr(t, err)
	defer DeleteExecution(t, client, execution)

	tools.PrintResource(t, execution)
}

func TestExecutionsList(t *testing.T) {
	client, err := clients.NewWorkflowV2Client()
	th.AssertNoErr(t, err)

	workflow, err := CreateWorkflow(t, client)
	th.AssertNoErr(t, err)
	defer DeleteWorkflow(t, client, workflow)

	execution, err := CreateExecution(t, client, workflow)
	th.AssertNoErr(t, err)
	defer DeleteExecution(t, client, execution)

	list, err := ListExecutions(t, client, &executions.ListOpts{
		Description: &executions.ListFilter{
			Value: execution.Description,
		},
		CreatedAt: &executions.ListDateFilter{
			Filter: executions.FilterGTE,
			Value:  execution.CreatedAt,
		},
		Input: execution.Input,
	})

	th.AssertNoErr(t, err)
	th.AssertEquals(t, 1, len(list))

	tools.PrintResource(t, list)
}
