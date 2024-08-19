// Proof of Concepts of CB-Spider.
// The CB-Spider is a sub-Framework of the Cloud-Barista Multi-Cloud Project.
// The CB-Spider Mission is to connect all the clouds with a single interface.
//
//      * Cloud-Barista: https://github.com/cloud-barista
//
// This is a Cloud Driver Example for PoC Test.
//
// by hyokyung.kim@innogrid.co.kr, 2019.07.

package resources

import (
	"context"
	"errors"
	"fmt"
	"github.com/Azure/azure-sdk-for-go/sdk/monitor/azquery"
	"github.com/Azure/azure-sdk-for-go/sdk/resourcemanager/compute/armcompute/v6"
	"strconv"
	"strings"
	"time"

	call "github.com/cloud-barista/cb-spider/cloud-control-manager/cloud-driver/call-log"

	idrv "github.com/cloud-barista/cb-spider/cloud-control-manager/cloud-driver/interfaces"
	irs "github.com/cloud-barista/cb-spider/cloud-control-manager/cloud-driver/interfaces/resources"
)

type AzureMonitoringHandler struct {
	CredentialInfo idrv.CredentialInfo
	Region         idrv.RegionInfo
	Ctx            context.Context
	Client         *armcompute.VirtualMachinesClient
	MetricClient   *azquery.MetricsClient
}

var availableIntervalMinutes = []string{
	"1",
	"5",
	"15",
	"30",
	"60",
	"360",
	"720",
	"1440",
}

func toAzureIntervalMinute(intervalMinute string) (string, error) {
	switch intervalMinute {
	case availableIntervalMinutes[0]:
		return "PT1M", nil
	case availableIntervalMinutes[1]:
		return "PT5M", nil
	case availableIntervalMinutes[2]:
		return "PT15M", nil
	case availableIntervalMinutes[3]:
		return "PT30M", nil
	case availableIntervalMinutes[4]:
		return "PT1H", nil
	case availableIntervalMinutes[5]:
		return "PT6H", nil
	case availableIntervalMinutes[6]:
		return "PT12H", nil
	case availableIntervalMinutes[7]:
		return "P1D", nil
	default:
		return "", errors.New(fmt.Sprintf("Unsupported interval minute: %s. "+
			"Available interval miniutes: %s", intervalMinute, strings.Join(availableIntervalMinutes, ",")))
	}
}

func (monitoringHandler *AzureMonitoringHandler) GetVMMetricData(monitoringReqInfo irs.MonitoringReqInfo) (irs.MetricData, error) {
	intervalMinute, err := strconv.Atoi(monitoringReqInfo.IntervalMinute)
	if err != nil {
		if monitoringReqInfo.IntervalMinute == "" {
			monitoringReqInfo.IntervalMinute = "1"
			intervalMinute = 1
		} else {
			return irs.MetricData{}, errors.New("invalid value of IntervalMinute")
		}
	}

	interval, err := toAzureIntervalMinute(monitoringReqInfo.IntervalMinute)
	if err != nil {
		return irs.MetricData{}, err
	}

	timeBeforeHour, err := strconv.Atoi(monitoringReqInfo.TimeBeforeHour)
	if err != nil {
		if monitoringReqInfo.TimeBeforeHour == "" {
			monitoringReqInfo.TimeBeforeHour = "1"
			timeBeforeHour = 1
		} else {
			return irs.MetricData{}, errors.New("invalid value of TimeBeforeHour")
		}
	}
	if timeBeforeHour < 0 {
		return irs.MetricData{}, errors.New("invalid value of TimeBeforeHour")
	}

	if timeBeforeHour*60 < intervalMinute {
		return irs.MetricData{}, errors.New("IntervalMinute is too far in the past")
	}

	// log HisCall
	hiscallInfo := GetCallLogScheme(monitoringHandler.Region, call.MONITORING, monitoringReqInfo.VMIID.NameId, "GetVMMonitoring()")
	start := call.Start()

	convertedIID, err := ConvertVMIID(monitoringReqInfo.VMIID, monitoringHandler.CredentialInfo, monitoringHandler.Region)
	if err != nil {
		getErr := errors.New(fmt.Sprintf("Failed to get metric data. err = %s", err))
		cblogger.Error(getErr.Error())
		LoggingError(hiscallInfo, getErr)
		return irs.MetricData{}, getErr
	}

	vm, err := GetRawVM(convertedIID, monitoringHandler.Region.Region, monitoringHandler.Client, monitoringHandler.Ctx)
	if err != nil {
		getErr := errors.New(fmt.Sprintf("Failed to get metric data. err = %s", err))
		cblogger.Error(getErr.Error())
		LoggingError(hiscallInfo, getErr)
		return irs.MetricData{}, getErr
	}

	endTime := time.Now().UTC()
	startTime := endTime.Add(time.Duration(-timeBeforeHour) * time.Hour)
	timespan := azquery.TimeInterval(fmt.Sprintf("%s/%s", startTime.Format(time.RFC3339), endTime.Format(time.RFC3339)))

	var metricName = "Percentage CPU" // irs.CPUUsage
	var aggregation = azquery.AggregationTypeAverage

	switch monitoringReqInfo.MetricType {
	case irs.MemoryUsage:
		metricName = "Available Memory Bytes"
	case irs.DiskRead:
		metricName = "Disk Read Bytes"
		aggregation = azquery.AggregationTypeTotal
	case irs.DiskWrite:
		metricName = "Disk Write Bytes"
		aggregation = azquery.AggregationTypeTotal
	case irs.DiskReadOps:
		metricName = "Disk Read Operations/Sec"
	case irs.DiskWriteOps:
		metricName = "Disk Write Operations/Sec"
	case irs.NetworkIn:
		metricName = "Network In"
		aggregation = azquery.AggregationTypeTotal
	case irs.NetworkOut:
		metricName = "Network Out"
		aggregation = azquery.AggregationTypeTotal
	}

	metrics := make([]string, 0)
	metrics = append(metrics, metricName)
	metricNames := strings.Join(metrics, ",")
	resultType := azquery.ResultTypeData

	resp, err := monitoringHandler.MetricClient.QueryResource(context.Background(), *vm.ID, &azquery.MetricsClientQueryResourceOptions{
		Aggregation:     []*azquery.AggregationType{&aggregation},
		Filter:          nil,
		Interval:        toStrPtr(interval),
		MetricNames:     &metricNames,
		MetricNamespace: nil,
		OrderBy:         nil,
		ResultType:      &resultType,
		Timespan:        &timespan,
		Top:             nil,
	})
	if err != nil {
		getErr := errors.New(fmt.Sprintf("Failed to get metric data. err = %s", err))
		cblogger.Error(getErr.Error())
		LoggingError(hiscallInfo, getErr)
		return irs.MetricData{}, getErr
	}

	LoggingInfo(hiscallInfo, start)

	var vmMonitoringInfo irs.MetricData
	var timestampValues []irs.TimestampValue

	for i, metric := range resp.Value {
		if i == 0 && metric.Unit != nil {
			if metric.Name != nil && metric.Name.Value != nil {
				vmMonitoringInfo.MetricName = *metric.Name.Value
			}
			vmMonitoringInfo.MetricUnit = string(*metric.Unit)
		}

		for _, timeseries := range metric.TimeSeries {
			for _, val := range timeseries.MetadataValues {
				fmt.Println(*val.Name, *val.Value)
			}

			if timeseries.Data == nil {
				continue
			}
			for _, data := range timeseries.Data {
				timestamp := data.TimeStamp
				if timestamp == nil {
					continue
				}

				var value *float64
				if aggregation == azquery.AggregationTypeTotal {
					value = data.Total
				} else {
					value = data.Average
				}

				if value == nil {
					continue
				}

				timestampValues = append(timestampValues, irs.TimestampValue{
					Timestamp: *timestamp,
					Value:     strconv.FormatFloat(*value, 'f', -1, 64),
				})
			}
		}
	}

	vmMonitoringInfo.TimestampValues = timestampValues

	return vmMonitoringInfo, nil
}
