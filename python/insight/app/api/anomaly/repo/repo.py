from datetime import datetime

import pandas as pd
import pytz
from influxdb import InfluxDBClient
from sqlalchemy import update
from sqlalchemy.orm import Session

from app.api.anomaly.model.models import AgentPlugin, AnomalyDetectionSettings
from app.api.anomaly.request.req import GetAnomalyHistoryFilter
from config.ConfigManager import ConfigManager


class AnomalySettingsRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_plugin_info(self):
        return self.db.query(AgentPlugin).all()

    def get_all_settings(self):
        return self.db.query(AnomalyDetectionSettings).all()

    def get_specific_setting(self, ns_id: str, mci_id: str, vm_id: str | None = None):
        return self.db.query(AnomalyDetectionSettings).filter_by(NAMESPACE_ID=ns_id, MCI_ID=mci_id, VM_ID=vm_id).all()

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
        return (
            self.db.query(AnomalyDetectionSettings)
            .filter_by(
                NAMESPACE_ID=setting_data["NAMESPACE_ID"],
                MCI_ID=setting_data["MCI_ID"],
                VM_ID=setting_data.get("VM_ID", None),
                MEASUREMENT=setting_data["MEASUREMENT"],
            )
            .first()
        )


class InfluxDBRepository:
    def __init__(self):
        config = ConfigManager()
        db_info = config.get_influxdb_config()
        self.client = InfluxDBClient(
            host=db_info["host"],
            port=db_info["port"],
            username=db_info["username"],
            password=db_info["password"],
            database=db_info["database"],
        )

    def save_results(self, df: pd.DataFrame, setting: AnomalyDetectionSettings):
        tag = {
            "ns_id": setting.NAMESPACE_ID,
            "mci_id": setting.MCI_ID,
        }

        vm_id = getattr(setting, "VM_ID", None)
        if vm_id:
            tag["vm_id"] = vm_id

        json_body = []

        for _, row in df.iterrows():
            data_point = {
                "measurement": setting.MEASUREMENT.lower(),
                "tags": tag,
                "time": row["timestamp"],
                "fields": {"anomaly_score": row["anomaly_score"], "isAnomaly": int(row["isAnomaly"])},
            }
            json_body.append(data_point)

        self.client.write_points(json_body)

    def query_anomaly_detection_results(self, path_params, query_params: GetAnomalyHistoryFilter):
        ns_id = path_params.nsId
        mci_id = path_params.mciId
        vm_id = getattr(path_params, "vmId", None)
        measurement = query_params.measurement.value.lower()

        start_time = query_params.start_time
        end_time = query_params.end_time

        query = f'SELECT mean("anomaly_score") as "anomaly_score", mean("isAnomaly") as "isAnomaly" FROM "insight"."autogen".f"{measurement}"'

        conditions = []
        conditions.append(f"\"ns_id\" = '{ns_id}'")
        conditions.append(f"\"mci_id\" = '{mci_id}'")
        if vm_id:
            conditions.append(f"\"vm_id\" = '{vm_id}'")
        conditions.append(f"time >= '{start_time}'")
        conditions.append(f"time <= '{end_time}'")

        query += " WHERE " + " AND ".join(conditions)
        query += "GROUP BY time(1m) FILL(null)"

        results = self.client.query(query)
        points = list(results.get_points())

        parsed_results = []
        for point in points:
            parsed_results.append(
                {
                    "timestamp": point["time"],
                    "anomaly_score": point["anomaly_score"],
                    "isAnomaly": point["isAnomaly"],
                }
            )

        return parsed_results


class AnomalyServiceRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_anomaly_setting_info(self, seq: int):
        return self.db.query(AnomalyDetectionSettings).filter_by(SEQ=seq).first()

    def update_last_exe_time(self, seq: int):
        current_time_utc = datetime.now(pytz.UTC)

        stmt = (
            update(AnomalyDetectionSettings)
            .where(AnomalyDetectionSettings.SEQ == seq)
            .values(LAST_EXECUTION=current_time_utc)
        )

        self.db.execute(stmt)
        self.db.commit()
