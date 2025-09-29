from app.core.dependencies.db import engine

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter as OTLPSpanExporterHTTP
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.instrumentation.logging import LoggingInstrumentor

from opentelemetry.sdk.trace import TracerProvider, ReadableSpan
from opentelemetry.sdk.trace.export import BatchSpanProcessor, SpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.trace import SpanKind

from fastapi import FastAPI
import platform
import os

SERVICE_NAME = os.environ.get('OTEL_SERVICE_NAME')
MODE = os.environ.get('mode')
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = os.environ.get("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://mc-observability-tempo:4318/v1/traces")

class MySpanProcessor(BatchSpanProcessor):
    def __init__(self, span_exporter: SpanExporter):
        super().__init__(span_exporter)
        self.exclude_type = ['http.request', 'http.response.start', 'http.response.body']

    def on_end(self, span: ReadableSpan) -> None:
        if span.kind == SpanKind.INTERNAL and (
                span.attributes.get('type', None) in ('http.request',
                                                      'http.response.start',
                                                      'http.response.body')
            or span.attributes.get('asgi.event.type', None) in ('http.request'
                                                                'http.response.start',
                                                                'http.response.body')
        ):
            return
        super().on_end(span=span)


def init_otel_trace(app) -> None:
    tracer = TracerProvider(
        resource=Resource({"service.name": SERVICE_NAME, "service.instance.id": platform.uname().node})
    )

    if MODE == "otlp-http":
        tracer.add_span_processor(
            MySpanProcessor(OTLPSpanExporterHTTP(endpoint=OTEL_EXPORTER_OTLP_TRACES_ENDPOINT))
        )

    LoggingInstrumentor().instrument()
    SQLAlchemyInstrumentor().instrument(engine=engine, tracer_provider=tracer)
    FastAPIInstrumentor.instrument_app(app, tracer_provider=tracer)
