import os
import json
import logging
import platform
from datetime import datetime
from opentelemetry.sdk._logs import LoggerProvider, LoggingHandler
from opentelemetry.sdk._logs.export import BatchLogRecordProcessor, LogExporter
from opentelemetry.sdk.resources import Resource
from datetime import timezone

SERVICE_NAME = os.environ.get('OTEL_SERVICE_NAME', 'mc-observability-insight')


class FileLogExporter(LogExporter):
    def __init__(self, filepath='./log/mc-observability-insight.log'):
        self._file = open(filepath, "a", encoding="utf-8")

    def export(self, batch):
        for log_data in batch:
            log_record = log_data.log_record
            custom_log = self._build_custom_log(log_record)
            self._file.write(json.dumps(custom_log, ensure_ascii=False) + "\n")
        self._file.flush()
        return True

    def shutdown(self):
        self._file.close()

    def _build_custom_log(self, log_record):
        """Build custom log structure matching manager log format"""
        body = str(log_record.body)

        custom_log = self._create_base_log_structure(log_record, body)

        if log_record.attributes:
            self._add_code_location_attributes(custom_log, log_record.attributes)
            body = self._enrich_body_with_exception_info(body, log_record.attributes)
            custom_log["body"] = body

        return custom_log

    def _create_base_log_structure(self, log_record, body):
        """Create base log structure with trace info and severity"""
        return {
            "timestamp": self._format_timestamp(log_record.timestamp),
            "trace_id": self._format_trace_id(log_record.trace_id),
            "span_id": self._format_span_id(log_record.span_id),
            "trace_flags": log_record.trace_flags,
            "severity_text": log_record.severity_text or "INFO",
            "attributes": {
                "component": SERVICE_NAME,
            },
            "body": body
        }

    @staticmethod
    def _format_timestamp(timestamp_ns):
        """Convert nanosecond timestamp to ISO format in UTC with milliseconds"""
        dt = datetime.fromtimestamp(timestamp_ns / 1e9, tz=timezone.utc)
        return dt.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'

    @staticmethod
    def _format_trace_id(trace_id):
        """Format trace ID as 32-character hex string"""
        return format(trace_id, '032x') if trace_id else ""

    @staticmethod
    def _format_span_id(span_id):
        """Format span ID as 16-character hex string"""
        return format(span_id, '016x') if span_id else ""

    @staticmethod
    def _add_code_location_attributes(custom_log, attributes):
        """Extract and add code location information to log attributes"""
        code_file = attributes.get("code.file.path", "")
        if code_file:
            custom_log["attributes"]["code.file"] = os.path.basename(code_file)

        code_function = attributes.get("code.function.name")
        if code_function:
            custom_log["attributes"]["code.function"] = code_function

        code_line = attributes.get("code.line.number")
        if code_line:
            custom_log["attributes"]["code.lineno"] = str(code_line)

    def _enrich_body_with_exception_info(self, body, attributes):
        """Append exception information to log body if available"""
        exception_stacktrace = attributes.get("exception.stacktrace")
        if exception_stacktrace:
            return body + "\n" + exception_stacktrace

        exception_type = attributes.get("exception.type")
        exception_message = attributes.get("exception.message")

        if exception_type or exception_message:
            exception_info = self._format_exception_info(exception_type, exception_message)
            return body + "\n" + exception_info

        return body

    @staticmethod
    def _format_exception_info(exception_type, exception_message):
        """Format exception type and message into readable string"""
        if exception_type and exception_message:
            return f"{exception_type}: {exception_message}"
        return exception_type or exception_message


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
