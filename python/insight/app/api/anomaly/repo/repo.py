from sqlalchemy.orm import Session
from app.api.anomaly.model.models import AnomalyDetectionSettings as model_ADS


def repo_get_all_settings(db: Session):
    return db.query(model_ADS).all()
