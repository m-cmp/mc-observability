// Cloud Control Manager's Rest Runtime of CB-Spider.
// The CB-Spider is a sub-Framework of the Cloud-Barista Multi-Cloud Project.
// The CB-Spider Mission is to connect all the clouds with a single interface.
//
//      * Cloud-Barista: https://github.com/cloud-barista
//
// by CB-Spider Team, 2020.09.

package commonruntime

import (
	ccm "github.com/cloud-barista/cb-spider/cloud-control-manager"
	cres "github.com/cloud-barista/cb-spider/cloud-control-manager/cloud-driver/interfaces/resources"
	infostore "github.com/cloud-barista/cb-spider/info-store"
)

//================ Monitoring Handler

func GetVMMetricData(connectionName string, nameID string, metricType cres.MetricType, periodMinute string, timeBeforeHour string) (*cres.MetricData, error) {
	cblog.Info("call GetMetricData()")

	// check empty and trim user inputs
	connectionName, err := EmptyCheckAndTrim("connectionName", connectionName)
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	nameID, err = EmptyCheckAndTrim("nameID", nameID)
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	cldConn, err := ccm.GetCloudConnection(connectionName)
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	vmHandler, err := cldConn.CreateVMHandler()
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	vmSPLock.RLock(connectionName, nameID)
	defer vmSPLock.RUnlock(connectionName, nameID)

	// (1) get IID(NameId)
	var iidInfo VMIIDInfo
	err = infostore.GetByConditions(&iidInfo, CONNECTION_NAME_COLUMN, connectionName, NAME_ID_COLUMN, nameID)
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	// (2) get resource(SystemId)
	vm, err := vmHandler.GetVM(getDriverIID(cres.IID{NameId: iidInfo.NameId, SystemId: iidInfo.SystemId}))
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	// (3) set ResourceInfo(IID.NameId)
	// set ResourceInfo
	vm.IId = getUserIID(cres.IID{NameId: iidInfo.NameId, SystemId: iidInfo.SystemId})

	err = getSetNameId(connectionName, &vm)
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	monitoringHandler, err := cldConn.CreateMonitoringHandler()
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	// (4) get monitoring info
	info, err := monitoringHandler.GetVMMetricData(cres.MonitoringReqInfo{
		VMIID:          vm.IId,
		MetricType:     metricType,
		IntervalMinute: periodMinute,
		TimeBeforeHour: timeBeforeHour,
	})
	if err != nil {
		cblog.Error(err)
		return nil, err
	}

	return &info, nil
}
