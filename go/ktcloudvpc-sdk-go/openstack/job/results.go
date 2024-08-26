package job

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

type jobResult struct {
	gophercloud.Result
}

// KT Cloud D1 Platfrom API (Networking : Get Job Statue with the ID of it) : https://cloud.kt.com/docs/open-api-guide/d/computing/networking
type JobResult struct {												// Modified
	// Description of the request result 
	IpAddress string `json:"ipaddress"`

	// Description of the request result 
	Displaytext string `json:"displaytext"`

	// Indicates success or not
	Success bool `json:"success"`

	// Specifies the task ID.
	Id string `json:"id"`
}

type AsyncJobResult struct {   										// Added
	JobResult JobResult `json:"result"`
	
	//Specifies the task status.
	 //  0 : Progress: indicates that the task is in progress.
	 //  1 : Success: indicates the task is successfully executed.
	 //  2 : Fail: indicates that the task failed.
	JobState int `json:"state"`
}

// JobExecResult represents the result of a get operation. Call its ExtractJobResult
// method to interpret it as a jobresult.
type JobExecResult struct {
	jobResult
}

// ExtractJobResult is a function that accepts a result and extracts a jobresult.
func (r jobResult) ExtractJobResult() (*AsyncJobResult, error) { 	// Modified
	var s struct {
		AsyncJobResult *AsyncJobResult `json:"nc_queryasyncjobresultresponse"`
	}
	err := r.ExtractInto(&s)
	return s.AsyncJobResult, err
}
