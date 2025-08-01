from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from config.ConfigManager import ConfigManager

config = ConfigManager()
db_info = config.get_db_config()
database_url = f"mysql+pymysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}"

engine = create_engine(database_url)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
