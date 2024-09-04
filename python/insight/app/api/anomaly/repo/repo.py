from sqlalchemy.orm import Session
from app.api.anomaly.model.models import AnomalyDetectionSettings


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
