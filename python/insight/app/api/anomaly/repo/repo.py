from sqlalchemy.orm import Session
from app.api.anomaly.model.models import AnomalyDetectionSettings
from config.ConfigManager import read_influxdb_config
from influxdb import InfluxDBClient
from typing import Dict


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
            METRIC_TYPE=setting_data['METRIC_TYPE']
        ).first()


class InfluxDBRepository:
    def __init__(self):
        db_info = read_influxdb_config()
        self.client = InfluxDBClient(host=db_info['host'], port=db_info['port'], username=db_info['username'],
                                     password=db_info['password'], database=db_info['database'])

    def query_anomaly_detection_results(self, path_params: Dict, query_params: Dict):
        ns_id = path_params.get('nsId')
        target_id = path_params.get('targetId')
        metric_type = query_params.get('metric_type')
        start_time = query_params.get('start_time')
        end_time = query_params.get('end_time')

        # Construct the InfluxQL query
        influxql_query = f'''
        SELECT anomaly_score, anomaly_act, value
        FROM "{metric_type}"
        WHERE "namespace_id" = '{ns_id}'
        AND "target_id" = '{target_id}'
        AND time >= '{start_time}'
        AND time <= '{end_time}'
        '''

        # Execute the query
        results = self.client.query(influxql_query)
        points = list(results.get_points())

        # Parse the results
        parsed_results = []
        for point in points:
            parsed_results.append({
                "timestamp": point['time'],
                "anomaly_score": point.get('anomaly_score'),
                "anomaly_act": point.get('anomaly_act'),
                "value": point.get('value')
            })

        return parsed_results


class AnomalyServiceRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_anomaly_setting_info(self, seq: int):
        return self.db.query(AnomalyDetectionSettings).filter_by(SEQ=seq).first()
