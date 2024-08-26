package images

import (
	"encoding/json"
	"fmt"
	"reflect"
	"strings"
	"time"

	"github.com/cloud-barista/ktcloudvpc-sdk-go"
	"github.com/cloud-barista/ktcloudvpc-sdk-go/pagination"
)

// Image represents an image found in the KT Cloud Image service.
type Image struct {												// Modified
	// ID is the image UUID.
	ID 					string `json:"id"`

	// Name is the human-readable display name for the image.
	Name 				string `json:"name"`

	// DiskFormat is the format of the disk.
	// If set, valid values are ami, ari, aki, vhd, vmdk, raw, qcow2, vdi,
	// and iso.
	DiskFormat 			string `json:"disk_format"`

	// ContainerFormat is the format of the container.
	// Valid values are ami, ari, aki, bare, and ovf.
	ContainerFormat 	string `json:"container_format"`

	// Visibility defines who can see/use the image.
	Visibility 			ImageVisibility `json:"visibility"`

	// SizeBytes is the size of the data that's associated with the image.
	SizeBytes 			int64 `json:"size"`
	
	// VirtualSize is the virtual size of the image
	VirtualSize 		int64 `json:"virtual_size"`

	// Status is the image status. It can be "queued" or "active"
	// See imageservice/v2/images/type.go
	Status 				ImageStatus `json:"status"`

	// Checksum is the checksum of the data that's associated with the image.
	Checksum 			string `json:"checksum"`
	
	// Protected is whether the image is deletable or not.
	Protected 			bool `json:"protected"`
	
	// MinRAMMegabytes [optional] is the amount of RAM in MB that is required to
	// boot the image.
	MinRAMMegabytes 	int `json:"min_ram"`

	// MinDiskGigabytes is the amount of disk space in GB that is required to
	// boot the image.
	MinDiskGigabytes 	int `json:"min_disk"`

	// Owner is the tenant ID the image belongs to.
	Owner 				string `json:"owner"`

	// Hidden is whether the image is listed in default image list or not.
	Hidden 				bool `json:"os_hidden"`

	OsHashAlgo      	string `json:"os_hash_algo"`

	OsHashValue     	string `json:"os_hash_value"`

	// CreatedAt is the date when the image has been created.
	CreatedAt 			time.Time `json:"created_at"`

	// UpdatedAt is the date when the last change has been made to the image or
	// it's properties.
	UpdatedAt 			time.Time `json:"updated_at"`
	
	// Metadata is a set of metadata associated with the image.
	// Image metadata allow for meaningfully define the image properties
	// and tags.
	// See http://docs.openstack.org/developer/glance/metadefs-concepts.html.
	Locations       	[]struct {
		URL      string `json:"url"`
		Metadata struct {
			Store string `json:"store"`
		} `json:"metadata"`
	} `json:"locations"`

	DirectURL 			string   `json:"direct_url"`

	// Tags is a list of image tags. Tags are arbitrarily defined strings
	// attached to an image.
	Tags 				[]string `json:"tags"`

	Self      			string   `json:"self"`

	// File is the trailing path after the glance endpoint that represent the
	// location of the image or the path to retrieve it.
	File 				string `json:"file"`

	// Schema is the path to the JSON-schema that represent the image or image
	// entity.
	Schema 				string `json:"schema"`	

	Stores    			string   `json:"stores"`
}

func (r *Image) UnmarshalJSON(b []byte) error {
	type tmp Image
	var s struct {
		tmp
		SizeBytes                   interface{} `json:"size"`
		OpenStackImageImportMethods string      `json:"openstack-image-import-methods"`
		OpenStackImageStoreIDs      string      `json:"openstack-image-store-ids"`
	}
	err := json.Unmarshal(b, &s)
	if err != nil {
		return err
	}
	*r = Image(s.tmp)

	switch t := s.SizeBytes.(type) {
	case nil:
		r.SizeBytes = 0
	case float32:
		r.SizeBytes = int64(t)
	case float64:
		r.SizeBytes = int64(t)
	default:
		return fmt.Errorf("Unknown type for SizeBytes: %v (value: %v)", reflect.TypeOf(t), t)
	}

	// Bundle all other fields into Properties
	var result interface{}
	err = json.Unmarshal(b, &result)
	if err != nil {
		return err
	}
	return err
}

type commonResult struct {
	gophercloud.Result
}

// Extract interprets any commonResult as an Image.
func (r commonResult) Extract() (*Image, error) {
	var s *Image
	if v, ok := r.Body.(map[string]interface{}); ok {
		for k, h := range r.Header {
			if strings.ToLower(k) == "openstack-image-import-methods" {
				for _, s := range h {
					v["openstack-image-import-methods"] = s
				}
			}
			if strings.ToLower(k) == "openstack-image-store-ids" {
				for _, s := range h {
					v["openstack-image-store-ids"] = s
				}
			}
		}
	}
	err := r.ExtractInto(&s)
	return s, err
}

// CreateResult represents the result of a Create operation. Call its Extract
// method to interpret it as an Image.
type CreateResult struct {
	commonResult
}

// UpdateResult represents the result of an Update operation. Call its Extract
// method to interpret it as an Image.
type UpdateResult struct {
	commonResult
}

// GetResult represents the result of a Get operation. Call its Extract
// method to interpret it as an Image.
type GetResult struct {
	commonResult
}

// DeleteResult represents the result of a Delete operation. Call its
// ExtractErr method to interpret it as an Image.
type DeleteResult struct {
	gophercloud.ErrResult
}

// ImagePage represents the results of a List request.
type ImagePage struct {
	serviceURL string
	pagination.LinkedPageBase
}

// IsEmpty returns true if an ImagePage contains no Images results.
func (r ImagePage) IsEmpty() (bool, error) {
	images, err := ExtractImages(r)
	return len(images) == 0, err
}

// NextPageURL uses the response's embedded link reference to navigate to
// the next page of results.
func (r ImagePage) NextPageURL() (string, error) {
	var s struct {
		Next string `json:"next"`
	}
	err := r.ExtractInto(&s)
	if err != nil {
		return "", err
	}

	if s.Next == "" {
		return "", nil
	}

	return nextPageURL(r.serviceURL, s.Next)
}

// ExtractImages interprets the results of a single page from a List() call,
// producing a slice of Image entities.
func ExtractImages(r pagination.Page) ([]Image, error) {
	var s struct {
		Images []Image `json:"images"`
	}
	err := (r.(ImagePage)).ExtractInto(&s)
	return s.Images, err
}

// splitFunc is a helper function used to avoid a slice of empty strings.
func splitFunc(c rune) bool {
	return c == ','
}
