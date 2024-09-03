from sqlalchemy.orm import Session
from app.api.anomaly.model.models import AnomalyDetectionSettings as model_ADS


def repo_get_all_settings(db: Session):
    return db.query(model_ADS).all()


def repo_get_specific_setting(db: Session, ns_id: str, target_id: str):
    return db.query(model_ADS).filter_by(NAMESPACE_ID=ns_id, TARGET_ID=target_id).all()


def repo_create_setting(db: Session, setting_data: dict):
    new_setting = model_ADS(**setting_data)
    db.add(new_setting)
    db.commit()
    db.refresh(new_setting)
    return new_setting


def repo_update_setting(db: Session, setting_seq: int, update_data: dict):
    setting = db.query(model_ADS).filter_by(SEQ=setting_seq).first()
    if setting:
        for key, value in update_data.items():
            setattr(setting, key.upper(), value)
        db.commit()
        db.refresh(setting)
        return setting
    return None


def repo_delete_setting(db: Session, setting_seq: int):
    setting = db.query(model_ADS).filter_by(SEQ=setting_seq).first()
    if setting:
        db.delete(setting)
        db.commit()
        return setting
    return None
