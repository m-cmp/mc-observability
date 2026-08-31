"""RCA endpoint descriptions."""

post_rca_query_description = {
    "api_description": "Run RCA with bounded trace, log, and metric evidence collection.",
    "response": {200: {"description": "RCA response"}},
}

get_rca_records_description = {
    "api_description": "List RCA records.",
    "response": {200: {"description": "RCA records"}},
}

get_rca_record_detail_description = {
    "api_description": "Get RCA detail by analysis ID.",
    "response": {200: {"description": "RCA record"}},
}
