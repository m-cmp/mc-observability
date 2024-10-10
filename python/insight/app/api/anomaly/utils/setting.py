from app.api.anomaly.repo.repo import AnomalySettingsRepository
from app.api.anomaly.response.res import ResBodyAnomalyDetectionSettings, ResBodyVoid, AnomalyDetectionSettings
from enum import Enum
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session


class AnomalySettingsService:
    def __init__(self, db: Session):
        self.repo = AnomalySettingsRepository(db=db)

    def get_all_settings(self) -> ResBodyAnomalyDetectionSettings:
        settings = self.repo.get_all_settings()

        results = [
            AnomalyDetectionSettings(
                seq=setting.SEQ,
                ns_id=setting.NAMESPACE_ID,
                target_id=setting.TARGET_ID,
                target_type=setting.TARGET_TYPE,
                measurement=setting.MEASUREMENT,
                execution_interval=setting.EXECUTION_INTERVAL,
                last_execution=setting.LAST_EXECUTION.strftime('%Y-%m-%dT%H:%M:%SZ'),
                create_at=setting.REGDATE.strftime('%Y-%m-%dT%H:%M:%SZ')
            )
            for setting in settings
        ]

        return ResBodyAnomalyDetectionSettings(data=results)

    def get_setting(self, ns_id: str, target_id: str) -> ResBodyAnomalyDetectionSettings | JSONResponse:
        settings = self.repo.get_specific_setting(ns_id=ns_id, target_id=target_id)
        if settings:
            results = [
                AnomalyDetectionSettings(
                    seq=setting.SEQ,
                    ns_id=setting.NAMESPACE_ID,
                    target_id=setting.TARGET_ID,
                    target_type=setting.TARGET_TYPE,
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
        if 'target_id' in setting_data:
            setting_data['TARGET_ID'] = setting_data.pop('target_id')

        setting_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                        setting_data.items()}

        duplicate = self.repo.check_duplicate(setting_data=setting_data)
        if duplicate:
            return JSONResponse(status_code=409, content={"rs_code": "409",
                                                          "rs_msg": "A record with the same namespace_id, target_id, "
                                                                   "target_type, and measurement already exists."})

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