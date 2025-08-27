from config.ConfigManager import ConfigManager

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker


config = ConfigManager()
db_info = config.get_db_config()
database_url = f"mysql+pymysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}"

# Enable connection health checks and recycling to avoid stale connections
# - pool_pre_ping: checks connection liveness before using it
# - pool_recycle: closes and recreates connections after N seconds (below MySQL wait_timeout)
engine = create_engine(
    database_url,
    pool_pre_ping=True,
    pool_recycle=1800,  # 30 minutes; adjust if DB wait_timeout is lower
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
