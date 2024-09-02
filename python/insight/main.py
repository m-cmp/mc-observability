from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.anomaly import anomaly
from app.api.prediction import prediction

from config.ConfigManager import make_log_file

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

app.include_router(anomaly.router)
app.include_router(prediction.router)


@app.on_event("startup")
async def startup_event():
    make_log_file()

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9001, log_config="config/log.ini")
