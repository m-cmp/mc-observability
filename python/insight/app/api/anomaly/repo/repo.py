from app.api.anomaly.model.models import AnomalyDetectionSettings
from app.api.anomaly.request.req import GetHistoryPathParams, GetAnomalyHistoryFilter
from config.ConfigManager import ConfigManager
from influxdb import InfluxDBClient
import pandas as pd
import pytz
from datetime import datetime
from sqlalchemy.orm import Session
from sqlalchemy import update


class AnomalySettingsRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_all_settings(self):
        return self.db.query(AnomalyDetectionSettings).all()

    def get_specific_setting(self, ns_id: str, target_id: str):
        return self.db.query(AnomalyDetectionSettings).filter_by(NAMESPACE_ID=ns_id, TARGET_ID=target_id).all()

    def create_setting(self, setting_data: dict):
        new_setting = AnomalyDetectionSettings(**setting_data)
        self.db.add(new_setting)
        self.db.commit()
        self.db.refresh(new_setting)
        return new_setting

    def update_setting(self, setting_seq: int, update_data: dict):
        setting = self.db.query(AnomalyDetectionSettings).filter_by(SEQ=setting_seq).first()
        if setting:
            for key, value in update_data.items():
                setattr(setting, key.upper(), value)
            self.db.commit()
            self.db.refresh(setting)
            return setting
        return None

    def delete_setting(self, setting_seq: int):
        setting = self.db.query(AnomalyDetectionSettings).filter_by(SEQ=setting_seq).first()
        if setting:
            self.db.delete(setting)
            self.db.commit()
            return setting
        return None

    def check_duplicate(self, setting_data: dict):
        return self.db.query(AnomalyDetectionSettings).filter_by(
            NAMESPACE_ID=setting_data['NAMESPACE_ID'],
            TARGET_ID=setting_data['TARGET_ID'],
            TARGET_TYPE=setting_data['TARGET_TYPE'],
            measurement=setting_data['measurement']
        ).first()


class InfluxDBRepository:
    def __init__(self):
        config = ConfigManager()
        db_info = config.get_influxdb_config()
        self.client = InfluxDBClient(host=db_info['host'], port=db_info['port'], username=db_info['username'],
                                     password=db_info['password'], database=db_info['database'])

    def save_results(self, df: pd.DataFrame, setting: AnomalyDetectionSettings):
        tag = {
            'namespace_id': setting.NAMESPACE_ID,
            'target_id': setting.TARGET_ID,
        }

        json_body = []

        for index, row in df.iterrows():
            data_point = {
                "measurement": setting.measurement.lower(),
                "tags": tag,
                "time": row['timestamp'],
                "fields": {
                    "anomaly_score": row['anomaly_score'],
                    "isAnomaly": int(row['isAnomaly'])
                }
            }
            json_body.append(data_point)

        self.client.write_points(json_body)

    def query_anomaly_detection_results(self, path_params: GetHistoryPathParams, query_params: GetAnomalyHistoryFilter):
        ns_id = path_params.nsId
        target_id = path_params.targetId
        measurement = query_params.measurement.value.lower()

        start_time = query_params.start_time
        end_time = query_params.end_time

        influxql_query = f'''
        SELECT mean("anomaly_score") as "anomaly_score", mean("isAnomaly") as "isAnomaly" 
        FROM "insight"."autogen"."{measurement}" \
        WHERE "namespace_id" = '{ns_id}' \
        AND "target_id" = '{target_id}' \
        AND time >= '{start_time}' \
        AND time <= '{end_time}'  \
        GROUP BY time(1m) FILL(null)
        '''

        results = self.client.query(influxql_query)
        points = list(results.get_points())

        parsed_results = []
        for point in points:
            parsed_results.append({
                "timestamp": point['time'],
                "anomaly_score": point['anomaly_score'],
                "isAnomaly": point['isAnomaly'],
            })

        return parsed_results


class AnomalyServiceRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_anomaly_setting_info(self, seq: int):
        return self.db.query(AnomalyDetectionSettings).filter_by(SEQ=seq).first()

    def update_last_exe_time(self, seq: int):
        kst = pytz.timezone('Asia/Seoul')
        current_time_kst = datetime.now(kst)

        stmt = (
            update(AnomalyDetectionSettings)
            .where(AnomalyDetectionSettings.SEQ == seq)
            .values(LAST_EXECUTION=current_time_kst)
        )

        self.db.execute(stmt)
        self.db.commit()

