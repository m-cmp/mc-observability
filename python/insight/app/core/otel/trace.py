import os
import platform

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter as OTLPSpanExporterHTTP
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
from opentelemetry.sdk.resources import Resource

# from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

from app.core.dependencies.db import engine

MODE = os.environ.get("MODE", "otlp-http")
OTLP_HTTP_ENDPOINT = os.environ.get("OTLP_HTTP_ENDPOINT", "http://192.168.110.214:4318/v1/traces")


def init_otel_trace(app) -> None:
    tracer = TracerProvider(resource=Resource({"service.name": "o11y-insight", "service.instance.id": platform.uname().node}))
    trace.set_tracer_provider(tracer)

    if MODE == "otlp-http":
        tracer.add_span_processor(BatchSpanProcessor(OTLPSpanExporterHTTP(endpoint=OTLP_HTTP_ENDPOINT)))

    SQLAlchemyInstrumentor().instrument(engine=engine)
    FastAPIInstrumentor.instrument_app(app, tracer_provider=tracer)
