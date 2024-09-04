from app.api.anomaly.repo.repo import AnomalySettingsRepository
from app.api.anomaly.response.res import ResBodyAnomalyDetectionSettings, ResBodyVoid, AnomalyDetectionSettings
from config.ConfigManager import read_db_config
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from fastapi.responses import JSONResponse
from enum import Enum


class AnomalySettingsService:
    def __init__(self, db: Session):
        self.repo = AnomalySettingsRepository(db=db)

    def get_all_settings(self) -> ResBodyAnomalyDetectionSettings:
        settings = self.repo.get_all_settings()

        results = [
            AnomalyDetectionSettings(
                seq=setting.SEQ,
                namespace_id=setting.NAMESPACE_ID,
                target_id=setting.TARGET_ID,
                target_type=setting.TARGET_TYPE,
                metric_type=setting.METRIC_TYPE,
                execution_interval=setting.EXECUTION_INTERVAL,
                last_execution=setting.LAST_EXECUTION,
                createAt=setting.REGDATE
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
                    namespace_id=setting.NAMESPACE_ID,
                    target_id=setting.TARGET_ID,
                    target_type=setting.TARGET_TYPE,
                    metric_type=setting.METRIC_TYPE,
                    execution_interval=setting.EXECUTION_INTERVAL,
                    last_execution=setting.LAST_EXECUTION,
                    createAt=setting.REGDATE
                )
                for setting in settings
            ]
            return ResBodyAnomalyDetectionSettings(data=results)
        return JSONResponse(
            status_code=404,
            content={"rsCode": "404", "rsMsg": "Target Not Found"}
        )

    def create_setting(self, setting_data: dict) -> ResBodyVoid | JSONResponse:
        if 'nsId' in setting_data:
            setting_data['NAMESPACE_ID'] = setting_data.pop('nsId')
        if 'targetId' in setting_data:
            setting_data['TARGET_ID'] = setting_data.pop('targetId')

        setting_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                        setting_data.items()}

        duplicate = self.repo.check_duplicate(setting_data=setting_data)
        if duplicate:
            return JSONResponse(status_code=409, content={"rsCode": "409",
                                                         "rsMsg": "A record with the same namespace_id, target_id, "
                                                                  "target_type, and metric_type already exists."})

        self.repo.create_setting(setting_data=setting_data)
        return ResBodyVoid(rsMsg="Target Registered Successfully")

    def update_setting(self, setting_seq: int, update_data: dict) -> ResBodyVoid | JSONResponse:
        update_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                       update_data.items()}
        updated_setting = self.repo.update_setting(setting_seq=setting_seq, update_data=update_data)
        if updated_setting:
            return ResBodyVoid(rsMsg="Setting Updated Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rsCode": "404", "rsMsg": "Target Not Found"}
            )

    def delete_setting(self, setting_seq: int) -> ResBodyVoid | JSONResponse:
        deleted_setting = self.repo.delete_setting(setting_seq=setting_seq)
        if deleted_setting:
            return ResBodyVoid(rsMsg="Setting Deleted Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rsCode": "404", "rsMsg": "Target Not Found"}
            )


def get_db():
    db_info = read_db_config()
    database_url = f"mysql+pymysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}"

    engine = create_engine(database_url)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
