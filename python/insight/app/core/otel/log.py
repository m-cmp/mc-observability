import os

from opentelemetry.sdk._logs import LoggerProvider, LoggingHandler
from opentelemetry.sdk._logs.export import BatchLogRecordProcessor, LogExporter
from opentelemetry.sdk.resources import Resource

import logging
import platform

SERVICE_NAME = os.environ.get('OTEL_SERVICE_NAME')


'''
Todo
커스텀 LogRecordFactory 구현
세부적인 로그 포맷 맞춰줘야 함

original_factory = logging.getLogRecordFactory()
def custom_log_record_factory(*args, **kwargs):
    record = original_factory(*args, **kwargs)
    if hasattr(record, "otelTraceID"):
        record.trace_id = record.otelTraceID
    if hasattr(record, "otelSpanID"):
        record.span_id = record.otelSpanID
    if hasattr(record, "otelServiceName"):
        record.service_name = record.otelServiceName
    if hasattr(record, "otelTraceSampled"):
        record.sampled = record.otelTraceSampled

    return record
'''

class FileLogExporter(LogExporter):
    def __init__(self, filepath='./log/otel-log.log'):
        self._file = open(filepath, "a", encoding="utf-8")

    def export(self, batch):
        for log_data in batch:
            self._file.write(log_data.log_record.to_json(indent=None) + "\n")
        self._file.flush()
        return True

    def shutdown(self):
        self._file.close()

def init_otel_logger():
    root_logger = logging.getLogger()
    logger_provider = LoggerProvider(
        resource=Resource.create(
            {
                "service.name": SERVICE_NAME,
                "service.instance.id": platform.uname().node,
            }
        ),
    )

    exporter = FileLogExporter()
    batch_log_record_processor = BatchLogRecordProcessor(exporter=exporter)
    logger_provider.add_log_record_processor(batch_log_record_processor)
    handler = LoggingHandler(level=logging.NOTSET, logger_provider=logger_provider)
    root_logger.setLevel(logging.INFO)
    root_logger.addHandler(handler)

    # Todo
    # 커스텀 LogRecordFactory 설정
    # logging.setLogRecordFactory(custom_log_record_factory)