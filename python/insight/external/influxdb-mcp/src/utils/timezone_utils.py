from __future__ import annotations

import json
import re
from datetime import datetime
from typing import Any, Dict, List

try:
    from zoneinfo import ZoneInfo
except Exception:  # pragma: no cover
    ZoneInfo = None  # type: ignore


_RFC3339_RE = re.compile(
    r"^(?P<date>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(?P<frac>\d+))?(?P<tz>Z|[+-]\d{2}:\d{2})?$"
)


def _parse_rfc3339_to_datetime(value: str) -> datetime:
    """
    Parse RFC3339 timestamps with arbitrary fractional second precision.

    - Supports trailing 'Z' or explicit offsets
    - Truncates/pads fractional seconds to microseconds for Python's datetime
    """
    match = _RFC3339_RE.match(value)
    if not match:
        # Best-effort fallback: replace Z and try fromisoformat
        return datetime.fromisoformat(value.replace("Z", "+00:00"))

    date_part = match.group("date")
    frac_part = match.group("frac") or ""
    tz_part = match.group("tz") or "+00:00"
    if tz_part == "Z":
        tz_part = "+00:00"

    if frac_part:
        micro = (frac_part[:6]).ljust(6, "0")
        dt_str = f"{date_part}.{micro}{tz_part}"
    else:
        dt_str = f"{date_part}{tz_part}"

    return datetime.fromisoformat(dt_str)


def convert_rfc3339_to_timezone(value: str, timezone_name: str) -> str:
    """
    Convert a single RFC3339 timestamp string to the given timezone.
    Returns ISO 8601 string with offset, e.g., "+09:00" for Asia/Seoul.
    """
    if not timezone_name:
        return value
    if ZoneInfo is None:
        return value
    dt = _parse_rfc3339_to_datetime(value)
    local_dt = dt.astimezone(ZoneInfo(timezone_name))
    return local_dt.isoformat()


def convert_influxdb_result_timezone(result_obj: Dict[str, Any], timezone_name: str) -> Dict[str, Any]:
    """
    Walk an InfluxDB JSON result object and convert 'time' columns from UTC to the given timezone.

    Expected shape:
    { "status": "success", "data": {"results": [ { "series": [ {"columns": [...], "values": [[time, ...], ...] } ] } ] } }
    """
    if not timezone_name or "data" not in result_obj:
        return result_obj

    results = result_obj.get("data", {}).get("results", [])
    for res in results:
        series_list: List[Dict[str, Any]] = res.get("series", []) or []
        for series in series_list:
            columns: List[str] = series.get("columns", []) or []
            if "time" not in columns:
                continue
            time_idx = columns.index("time")
            values: List[List[Any]] = series.get("values", []) or []
            for row in values:
                try:
                    time_val = row[time_idx]
                    if isinstance(time_val, str):
                        row[time_idx] = convert_rfc3339_to_timezone(time_val, timezone_name)
                except Exception:
                    # Skip on per-row conversion errors to avoid breaking the whole response
                    continue
    return result_obj


