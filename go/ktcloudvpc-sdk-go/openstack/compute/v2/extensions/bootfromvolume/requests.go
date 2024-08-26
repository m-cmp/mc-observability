package bootfromvolume

import (
	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/openstack/compute/v2/servers"
)

type (
	// DestinationType represents the type of medium being used as the
	// destination of the bootable device.
	DestinationType string

	// SourceType represents the type of medium being used as the source of the
	// bootable device.
	SourceType string
)

const (
	// DestinationLocal DestinationType is for using an ephemeral disk as the
	// destination.
	DestinationLocal DestinationType = "local"

	// DestinationVolume DestinationType is for using a volume as the destination.
	DestinationVolume DestinationType = "volume"

	// SourceBlank SourceType is for a "blank" or empty source.
	SourceBlank SourceType = "blank"

	// SourceImage SourceType is for using images as the source of a block device.
	SourceImage SourceType = "image"

	// SourceSnapshot SourceType is for using a volume snapshot as the source of
	// a block device.
	SourceSnapshot SourceType = "snapshot"

	// SourceVolume SourceType is for using a volume as the source of block
	// device.
	SourceVolume SourceType = "volume"
)

// BlockDevice is a structure with options for creating block devices in a
// server. The block device may be created from an image, snapshot, new volume,
// or existing volume. The destination may be a new volume, existing volume
// which will be attached to the instance, ephemeral disk, or boot device.
type BlockDevice struct {										// Modified
	// DestinationType is the type that gets created. Possible values are "volume"
	// and "local".
	DestinationType DestinationType `json:"destination_type,omitempty"`
	
	// BootIndex is the boot index. It defaults to 0.
	BootIndex int `json:"boot_index"`

	// SourceType must be one of: "volume", "snapshot", "image", or "blank".
	SourceType SourceType `json:"source_type" required:"true"`

	// VolumeSize is the size of the volume to create (in gigabytes). This can be
	// omitted for existing volumes.
	VolumeSize string `json:"volume_size,omitempty"`			// Modified

	VolumeType string `json:"volume_type,omitempty"`			// Added. Not mentioned in KT Cloud D platform API doc.

	// UUID is the unique identifier for the existing volume, snapshot, or
	// image (see above).
	UUID string `json:"uuid,omitempty"`
}

// CreateOptsExt is a structure that extends the server `CreateOpts` structure
// by allowing for a block device mapping.
type CreateOptsExt struct {
	servers.CreateOptsBuilder
	BlockDevice []BlockDevice `json:"block_device_mapping_v2,omitempty"`
}

// ToServerCreateMap adds the block device mapping option to the base server
// creation options.
func (opts CreateOptsExt) ToServerCreateMap() (map[string]interface{}, error) {
	base, err := opts.CreateOptsBuilder.ToServerCreateMap()
	if err != nil {
		return nil, err
	}

	if len(opts.BlockDevice) == 0 {
		err := gophercloud.ErrMissingInput{}
		err.Argument = "bootfromvolume.CreateOptsExt.BlockDevice"
		return nil, err
	}

	serverMap := base["server"].(map[string]interface{})

	blockDevice := make([]map[string]interface{}, len(opts.BlockDevice))

	for i, bd := range opts.BlockDevice {
		b, err := gophercloud.BuildRequestBody(bd, "")
		if err != nil {
			return nil, err
		}
		blockDevice[i] = b
	}
	serverMap["block_device_mapping_v2"] = blockDevice

	return base, nil
}

// Create requests the creation of a server from the given block device mapping.
func Create(client *gophercloud.ServiceClient, opts servers.CreateOptsBuilder) (r servers.CreateResult) {
	b, err := opts.ToServerCreateMap()
	if err != nil {
		r.Err = err
		return
	}
	resp, err := client.Post(createURL(client), b, &r.Body, &gophercloud.RequestOpts{
		OkCodes: []int{200, 202},
	})
	_, r.Header, r.Err = gophercloud.ParseResponse(resp, err)
	return
}
