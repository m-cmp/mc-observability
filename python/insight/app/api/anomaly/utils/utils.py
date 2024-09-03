from app.api.anomaly.repo.repo import repo_get_all_settings
from app.api.anomaly.response.res import ResBodyAnomalyDetectionSettings, AnomalyDetectionSettings
from config.ConfigManager import read_db_config
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session


class AnomalySettings:
    def __init__(self, db: Session):
        self.db = db

    def get_all_settings(self) -> ResBodyAnomalyDetectionSettings:
        rows = repo_get_all_settings(db=self.db)

        results = [
            AnomalyDetectionSettings(
                seq=row.SEQ,
                ns_id=row.NAMESPACE_ID,
                target_id=row.TARGET_ID,
                target_type=row.TARGET_TYPE,
                metric_type=row.METRIC_TYPE,
                execution_interval=row.EXECUTION_INTERVAL,
                last_execution=row.LAST_EXECUTION,
                createAt=row.REGDATE
            )
            for row in rows
        ]

        return ResBodyAnomalyDetectionSettings(data=results)


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
