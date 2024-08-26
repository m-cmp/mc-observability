package capsules

import (
	"fmt"

	"github.com/cloud-barista/ktcloudvpc-sdk-go"
)

type ErrInvalidDataFormat struct {
	gophercloud.BaseError
}

func (e ErrInvalidDataFormat) Error() string {
	return fmt.Sprintf("Data in neither json nor yaml format.")
}
