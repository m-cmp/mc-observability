package job

import (
	"strings"
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

// Querying the task status based on its job ID.
func GetJobResult(client *gophercloud.ServiceClient, id string) (*AsyncJobResult, error) {  // Added
	var r JobExecResult
	url := jobURL(client, id)
	url = strings.Replace(url, "=/", "=", 1)

	_, err := client.Get(url, &r.Body, nil)
	if err != nil {
		return nil, err
	}

	return r.ExtractJobResult()
}
