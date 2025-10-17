from app.api.anomaly.repo.repo import AnomalySettingsRepository
from app.api.anomaly.response.res import ResBodyAnomalyDetectionSettings, ResBodyVoid, AnomalyDetectionSettings
from enum import Enum
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from typing import Optional


class AnomalySettingsService:
    def __init__(self, db: Session):
        self.repo = AnomalySettingsRepository(db=db)

    def map_plugin_info(self, measurement_field_config, target_measurement=None):
        plugin_list = self.repo.get_plugin_info()
        plugin_dict = {}
        for plugin in plugin_list:
            plugin_dict[plugin.NAME] = plugin.SEQ

        if target_measurement:
            target_measurement = target_measurement.measurement
            for measurement in measurement_field_config:
                if measurement['measurement'] == target_measurement:
                    measurement['plugin_seq'] = plugin_dict[measurement['measurement']]
                    return measurement

        result_dict = []
        for measurement in measurement_field_config:
            measurement['plugin_seq'] = plugin_dict[measurement['measurement']]
            result_dict.append(measurement)
        return result_dict

    def get_all_settings(self) -> ResBodyAnomalyDetectionSettings:
        settings = self.repo.get_all_settings()

        results = [
            AnomalyDetectionSettings(
                seq=setting.SEQ,
                ns_id=setting.NAMESPACE_ID,
                mci_id=setting.MCI_ID,
                vm_id=setting.VM_ID,
                measurement=setting.MEASUREMENT,
                execution_interval=setting.EXECUTION_INTERVAL,
                last_execution=setting.LAST_EXECUTION.strftime('%Y-%m-%dT%H:%M:%SZ') if setting.LAST_EXECUTION else None,
                create_at=setting.REGDATE.strftime('%Y-%m-%dT%H:%M:%SZ')
            )
            for setting in settings
        ]

        return ResBodyAnomalyDetectionSettings(data=results)

    def get_setting(self, ns_id: str, mci_id: str, vm_id: Optional[str] = None) -> ResBodyAnomalyDetectionSettings | JSONResponse:
        settings = self.repo.get_specific_setting(ns_id=ns_id, mci_id=mci_id, vm_id=vm_id)
        if settings:
            results = [
                AnomalyDetectionSettings(
                    seq=setting.SEQ,
                    ns_id=setting.NAMESPACE_ID,
                    mci_id=setting.MCI_ID,
                    vm_id=setting.VM_ID,
                    measurement=setting.MEASUREMENT,
                    execution_interval=setting.EXECUTION_INTERVAL,
                    last_execution=setting.LAST_EXECUTION.strftime('%Y-%m-%dT%H:%M:%SZ'),
                    create_at=setting.REGDATE.strftime('%Y-%m-%dT%H:%M:%SZ')
                )
                for setting in settings
            ]
            return ResBodyAnomalyDetectionSettings(data=results)
        return JSONResponse(
            status_code=404,
            content={"rs_code": "404", "rs_msg": "Target Not Found"}
        )

    def create_setting(self, setting_data: dict) -> ResBodyVoid | JSONResponse:
        if 'ns_id' in setting_data:
            setting_data['NAMESPACE_ID'] = setting_data.pop('ns_id')
        if 'mci_id' in setting_data:
            setting_data['MCI_ID'] = setting_data.pop('mci_id')
        if 'vm_id' in setting_data:
            setting_data['VM_ID'] = setting_data.pop('vm_id')

        setting_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                        setting_data.items()}

        duplicate = self.repo.check_duplicate(setting_data=setting_data)
        if duplicate:
            return JSONResponse(status_code=409, content={"rs_code": "409",
                                                          "rs_msg": "A record with the same id and measurement already exists."})


        self.repo.create_setting(setting_data=setting_data)
        return ResBodyVoid(rs_msg="Target Registered Successfully")

    def update_setting(self, setting_seq: int, update_data: dict) -> ResBodyVoid | JSONResponse:
        update_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                       update_data.items()}
        updated_setting = self.repo.update_setting(setting_seq=setting_seq, update_data=update_data)
        if updated_setting:
            return ResBodyVoid(rs_msg="Setting Updated Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rs_code": "404", "rs_msg": "Target Not Found"}
            )

    def delete_setting(self, setting_seq: int) -> ResBodyVoid | JSONResponse:
        deleted_setting = self.repo.delete_setting(setting_seq=setting_seq)
        if deleted_setting:
            return ResBodyVoid(rs_msg="Setting Deleted Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rs_code": "404", "rs_msg": "Target Not Found"}
            )