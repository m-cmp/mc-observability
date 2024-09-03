from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from app.api.anomaly import anomaly
from app.api.prediction import prediction
from config.ConfigManager import read_prefix

from config.ConfigManager import read_prefix

import uvicorn


app = FastAPI(
    title='Insight Module DOCS',
    description='mc-observability insight module'
)

origins = ['*']

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=['*'],
    allow_headers=['*']
)

api_prefix = read_prefix()
app.include_router(anomaly.router, prefix=api_prefix, tags=["[Insight] Anomaly Detection"])
app.include_router(prediction.router, prefix=api_prefix, tags=["[Insight] Prediction"])


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9001, log_config="config/log.ini")
