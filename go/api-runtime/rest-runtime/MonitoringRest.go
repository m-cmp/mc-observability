// Cloud Control Manager's Rest Runtime of CB-Spider.
// The CB-Spider is a sub-Framework of the Cloud-Barista Multi-Cloud Project.
// The CB-Spider Mission is to connect all the clouds with a single interface.
//
//      * Cloud-Barista: https://github.com/cloud-barista
//
// by CB-Spider Team, 2020.

package restruntime

import (
	cmrt "github.com/cloud-barista/cb-spider/api-runtime/common-runtime"
	cres "github.com/cloud-barista/cb-spider/cloud-control-manager/cloud-driver/interfaces/resources"

	// REST API (echo)
	"net/http"

	"github.com/labstack/echo/v4"
)

//================ Monitoring Handler

func GetVMMetricData(c echo.Context) error {
	cblog.Info("call GetVMMetricData()")

	var req struct {
		ConnectionName string
		PeriodMinute   string
		TimeBeforeHour string
	}

	if err := c.Bind(&req); err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, err.Error())
	}

	// To support for Get-Query Param Type API
	if req.ConnectionName == "" {
		req.ConnectionName = c.QueryParam("ConnectionName")
	}

	strMetricType := c.Param("MetricType")
	metricType := cres.StringMetricType(strMetricType)
	if metricType == cres.Unknown {
		return echo.NewHTTPError(http.StatusInternalServerError, "Invalid Metric Type")
	}

	req.PeriodMinute = c.QueryParam("IntervalMinute")
	if req.PeriodMinute == "" {
		req.PeriodMinute = "1"
	}
	req.TimeBeforeHour = c.QueryParam("TimeBeforeHour")
	if req.TimeBeforeHour == "" {
		req.TimeBeforeHour = "1"
	}

	// Call common-runtime API
	result, err := cmrt.GetVMMetricData(req.ConnectionName, c.Param("VMName"), metricType, req.PeriodMinute, req.TimeBeforeHour)
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, err.Error())
	}

	return c.JSON(http.StatusOK, result)
}

func GetClusterNodeMetricData(c echo.Context) error {
	cblog.Info("call GetClusterNodeMetricData()")

	var req struct {
		ConnectionName string
		PeriodMinute   string
		TimeBeforeHour string
	}

	if err := c.Bind(&req); err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, err.Error())
	}

	// To support for Get-Query Param Type API
	if req.ConnectionName == "" {
		req.ConnectionName = c.QueryParam("ConnectionName")
	}

	strMetricType := c.Param("MetricType")
	metricType := cres.StringMetricType(strMetricType)
	if metricType == cres.Unknown {
		return echo.NewHTTPError(http.StatusInternalServerError, "Invalid Metric Type")
	}

	req.PeriodMinute = c.QueryParam("IntervalMinute")
	if req.PeriodMinute == "" {
		req.PeriodMinute = "1"
	}
	req.TimeBeforeHour = c.QueryParam("TimeBeforeHour")
	if req.TimeBeforeHour == "" {
		req.TimeBeforeHour = "1"
	}

	// Call common-runtime API
	result, err := cmrt.GetClusterNodeMetricData(req.ConnectionName, c.Param("ClusterName"), c.Param("NodeGroupName"), c.Param("NodeNumber"), metricType, req.PeriodMinute, req.TimeBeforeHour)
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, err.Error())
	}

	return c.JSON(http.StatusOK, result)
}
